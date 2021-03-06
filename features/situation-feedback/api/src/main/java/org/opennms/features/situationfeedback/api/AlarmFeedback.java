/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2018 The OpenNMS Group, Inc.
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

package org.opennms.features.situationfeedback.api;

/**
 * Expresses Feedback on the Correlation of an Alarm.
 */
public class AlarmFeedback {

    public enum FeedbackType {
        FALSE_POSITVE, // Alarm does not belong in this Situation
        FALSE_NEGATIVE, // Alarm was missing from this Situation
        CORRECT, // Alarm is correctly correlated
        UNKNOWN;

        public static FeedbackType getType(String type) {
            switch(type) {
            case "FALSE_POSITVE": 
                return FALSE_POSITVE;
            case "FALSE_NEGATIVE":
                return FALSE_NEGATIVE;
            case "CORRECT":
                return CORRECT;
            default:
                return UNKNOWN;
            }
        }
    }

    // Situation ReductionKey
    private String situationKey;

    // this may not be nesc but may prove helpful
    private String situationFingerprint; // fingerprint of situation/alarms at
                                         // time of feedback;
    // Alarm ReductionKey
    private String alarmKey;

    private FeedbackType feedbackType;

    private String reason;

    private String user;

    private long timestamp;

    public AlarmFeedback() {
    }

    public AlarmFeedback(String situationKey, String situationFingerprint, String alarmKey, FeedbackType feedbackType, String reason, String user,
            long timestamp) {
        this.situationKey = situationKey;
        this.situationFingerprint = situationFingerprint;
        this.alarmKey = alarmKey;
        this.feedbackType = feedbackType;
        this.reason = reason;
        this.user = user;
        this.timestamp = timestamp;
    }

    public String getSituationKey() {
        return situationKey;
    }

    public String getSituationFingerprint() {
        return situationFingerprint;
    }

    public String getAlarmKey() {
        return alarmKey;
    }

    public FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public String getReason() {
        return reason;
    }

    public String getUser() {
        return user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Feedback[" + getFeedbackType() + ":" + getSituationKey() + ":" + getAlarmKey() + ":" + getReason() + "]";
    }

}
