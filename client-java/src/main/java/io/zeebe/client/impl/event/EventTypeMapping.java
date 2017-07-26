/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client.impl.event;

import io.zeebe.client.event.EventType;
import io.zeebe.protocol.clientapi.EventType;

public class EventTypeMapping
{
    protected static final EventType[] MAPPING;

    static
    {
        MAPPING = new EventType[EventType.values().length];
        MAPPING[EventType.TASK_EVENT.value()] = EventType.TASK;
        MAPPING[EventType.WORKFLOW_EVENT.value()] = EventType.WORKFLOW_INSTANCE;
        MAPPING[EventType.INCIDENT_EVENT.value()] = EventType.INCIDENT;
        MAPPING[EventType.RAFT_EVENT.value()] = EventType.RAFT;
    }

    public static EventType mapEventType(EventType protocolType)
    {
        if (protocolType.value() < MAPPING.length)
        {
            return MAPPING[protocolType.value()];
        }
        else if (protocolType != io.zeebe.protocol.clientapi.EventType.NULL_VAL)
        {
            return EventType.UNKNOWN;
        }
        else
        {
            return null;
        }
    }
}
