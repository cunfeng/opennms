/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.kafka.producer;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opennms.features.kafka.producer.model.OpennmsModelProtos;
import org.opennms.netmgt.alarmd.api.AlarmLifecycleListener;
import org.opennms.netmgt.alarmd.api.AlarmLifecycleSubscriptionService;
import org.opennms.netmgt.events.api.EventListener;
import org.opennms.netmgt.events.api.EventSubscriptionService;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.xml.event.Event;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class OpennmsKafkaProducer implements AlarmLifecycleListener, EventListener {
    private static final Logger LOG = LoggerFactory.getLogger(OpennmsKafkaProducer.class);

    private static final String KAFKA_PRODUCER_CLIENT_PID = "org.opennms.features.kafka.producer.client";

    private final ProtobufMapper protobufMapper;
    private final NodeCache nodeCache;
    private final ConfigurationAdmin configAdmin;
    private final EventSubscriptionService eventSubscriptionService;
    private final AlarmLifecycleSubscriptionService alarmLifecycleSubscriptionService;

    private String eventTopic;
    private String alarmTopic;
    private String nodeTopic;

    private boolean forwardEvents;
    private boolean forwardAlarms;
    private boolean forwardNodes;

    private KafkaProducer<String, byte[]> producer;

    public OpennmsKafkaProducer(ProtobufMapper protobufMapper, NodeCache nodeCache,
                                ConfigurationAdmin configAdmin, EventSubscriptionService eventSubscriptionService,
                                AlarmLifecycleSubscriptionService alarmLifecycleSubscriptionService) {
        this.protobufMapper = Objects.requireNonNull(protobufMapper);
        this.nodeCache = Objects.requireNonNull(nodeCache);
        this.configAdmin = Objects.requireNonNull(configAdmin);
        this.eventSubscriptionService = Objects.requireNonNull(eventSubscriptionService);
        this.alarmLifecycleSubscriptionService = Objects.requireNonNull(alarmLifecycleSubscriptionService);
    }

    public void init() throws IOException {
        // Create the Kafka producer
        final Properties producerConfig = new Properties();
        final Dictionary<String, Object> properties = configAdmin.getConfiguration(KAFKA_PRODUCER_CLIENT_PID).getProperties();
        if (properties != null) {
            final Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                final String key = keys.nextElement();
                producerConfig.put(key, properties.get(key));
            }
        }
        // Overwrite the serializers, since we rely on these
        producerConfig.put("key.serializer", StringSerializer.class.getCanonicalName());
        producerConfig.put("value.serializer", ByteArraySerializer.class.getCanonicalName());

        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Class-loader hack for accessing the org.apache.kafka.common.serialization.*
            Thread.currentThread().setContextClassLoader(null);
            producer = new KafkaProducer<>(producerConfig);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }

        if (forwardEvents) {
            eventSubscriptionService.addEventListener(this);
        }
        if (forwardAlarms) {
            alarmLifecycleSubscriptionService.addAlarmLifecyleListener(this);
        }
    }

    public void destroy() {
        if (producer != null) {
            producer.close();
            producer = null;
        }

        if (forwardEvents) {
            eventSubscriptionService.removeEventListener(this);
        }
        if (forwardAlarms) {
            alarmLifecycleSubscriptionService.removeAlarmLifecycleListener(this);
        }
    }

    private void forwardEvent(Event event) {
        if (forwardNodes && event.getNodeid() != null && event.getNodeid() != 0) {
            maybeUpdateNode(event.getNodeid());
        }
        sendRecord(() -> {

            final OpennmsModelProtos.Event mappedEvent = protobufMapper.toEvent(event).build();
            LOG.debug("Sending event with UEI: {}", mappedEvent.getUei());
            return new ProducerRecord<>(eventTopic, mappedEvent.getUei(), mappedEvent.toByteArray());
        });
    }

    private void updateAlarm(String reductionKey, OnmsAlarm alarm) {
        if (alarm == null) {
            // The alarm was deleted, push a null record to the reduction key
            sendRecord(() -> {
                LOG.debug("Deleting alarm with reduction key: {}", reductionKey);
                return new ProducerRecord<>(alarmTopic, reductionKey, null);
            });
            return;
        }

        if (forwardNodes && alarm.getNodeId() != null) {
            maybeUpdateNode(alarm.getNodeId());
        }
        sendRecord(() -> {
            final OpennmsModelProtos.Alarm mappedAlarm = protobufMapper.toAlarm(alarm).build();
            LOG.debug("Sending alarm with reduction key: {}", reductionKey);
            return new ProducerRecord<>(alarmTopic, reductionKey, mappedAlarm.toByteArray());
        });
    }

    private void maybeUpdateNode(long nodeId) {
        nodeCache.triggerIfNeeded(nodeId, (node) -> {
            final String nodeCriteria;
            if (node != null && node.getForeignSource() != null && node.getForeignId() != null) {
                nodeCriteria = String.format("%s:%s", node.getForeignSource(), node.getForeignId());
            } else {
                nodeCriteria = Long.toString(nodeId);
            }

            if (node == null) {
                // The node was deleted, push a null record
                sendRecord(() -> {
                    LOG.debug("Deleting node with criteria: {}", nodeCriteria);
                    return new ProducerRecord<>(nodeTopic, nodeCriteria, null);
                });
                return;
            }

            sendRecord(() -> {
                final OpennmsModelProtos.Node mappedNode = protobufMapper.toNode(node).build();
                LOG.debug("Sending node with criteria: {}", nodeCriteria);
                return new ProducerRecord<>(nodeTopic, nodeCriteria, mappedNode.toByteArray());
            });
        });
    }

    private void sendRecord(Callable<ProducerRecord<String,byte[]>> callable) {
        if (producer == null) {
            return;
        }

        final ProducerRecord<String,byte[]> record;
        try {
            record = callable.call();
        } catch (Exception e) {
            // Propagate
            throw new RuntimeException(e);
        }

        producer.send(record, (recordMetadata, e) -> {
            if (e != null) {
                LOG.warn("Failed to send record to producer: {}.", record, e);
            }
        });
    }

    @Override
    public void handleNewOrUpdatedAlarm(OnmsAlarm alarm) {
        updateAlarm(alarm.getReductionKey(), alarm);
    }

    @Override
    public void handleDeletedAlarm(int alarmId, String reductionKey) {
        updateAlarm(reductionKey, null);
    }

    @Override
    public String getName() {
        return OpennmsKafkaProducer.class.getName();
    }

    @Override
    public void onEvent(Event event) {
        forwardEvent(event);
    }

    public void setEventTopic(String eventTopic) {
        this.eventTopic = eventTopic;
        forwardEvents = !Strings.isNullOrEmpty(eventTopic);
    }

    public void setAlarmTopic(String alarmTopic) {
        this.alarmTopic = alarmTopic;
        forwardAlarms = !Strings.isNullOrEmpty(alarmTopic);
    }

    public void setNodeTopic(String nodeTopic) {
        this.nodeTopic = nodeTopic;
        forwardNodes = !Strings.isNullOrEmpty(nodeTopic);
    }
}