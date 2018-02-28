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

package org.opennms.netmgt.telemetry.listeners.sflow.proto.flows;

import java.nio.ByteBuffer;

import org.opennms.netmgt.telemetry.listeners.api.utils.BufferUtils;
import org.opennms.netmgt.telemetry.listeners.sflow.InvalidPacketException;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.UnsignedLong;

// struct host_memory {
//     unsigned hyper mem_total;   /* total bytes */
//     unsigned hyper mem_free;    /* free bytes */
//     unsigned hyper mem_shared;  /* shared bytes */
//     unsigned hyper mem_buffers; /* buffers bytes */
//     unsigned hyper mem_cached;  /* cached bytes */
//     unsigned hyper swap_total;  /* swap total bytes */
//     unsigned hyper swap_free;   /* swap free bytes */
//     unsigned int page_in;       /* page in count */
//     unsigned int page_out;      /* page out count */
//     unsigned int swap_in;       /* swap in count */
//     unsigned int swap_out;      /* swap out count */
// };

public class HostMemory implements CounterData {
    public final UnsignedLong mem_total;
    public final UnsignedLong mem_free;
    public final UnsignedLong mem_shared;
    public final UnsignedLong mem_buffers;
    public final UnsignedLong mem_cached;
    public final UnsignedLong swap_total;
    public final UnsignedLong swap_free;
    public final long page_in;
    public final long page_out;
    public final long swap_in;
    public final long swap_out;

    public HostMemory(final ByteBuffer buffer) throws InvalidPacketException {
        this.mem_total = BufferUtils.uint64(buffer);
        this.mem_free = BufferUtils.uint64(buffer);
        this.mem_shared = BufferUtils.uint64(buffer);
        this.mem_buffers = BufferUtils.uint64(buffer);
        this.mem_cached = BufferUtils.uint64(buffer);
        this.swap_total = BufferUtils.uint64(buffer);
        this.swap_free = BufferUtils.uint64(buffer);
        this.page_in = BufferUtils.uint32(buffer);
        this.page_out = BufferUtils.uint32(buffer);
        this.swap_in = BufferUtils.uint32(buffer);
        this.swap_out = BufferUtils.uint32(buffer);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mem_total", mem_total)
                .add("mem_free", mem_free)
                .add("mem_shared", mem_shared)
                .add("mem_buffers", mem_buffers)
                .add("mem_cached", mem_cached)
                .add("swap_total", swap_total)
                .add("swap_free", swap_free)
                .add("page_in", page_in)
                .add("page_out", page_out)
                .add("swap_in", swap_in)
                .add("swap_out", swap_out)
                .toString();
    }
}