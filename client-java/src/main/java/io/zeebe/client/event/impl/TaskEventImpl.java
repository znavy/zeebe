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
package io.zeebe.client.event.impl;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.event.TopicEventType;
import io.zeebe.client.task.impl.subscription.MsgPackField;
import io.zeebe.protocol.Protocol;

public class TaskEventImpl extends EventImpl implements TaskEvent
{

    protected Map<String, Object> headers = new HashMap<>();
    protected long lockTime = Protocol.INSTANT_NULL_VALUE;
    protected String lockOwner;
    protected Integer retries;
    protected String type;
    protected final MsgPackField payload = new MsgPackField();

    @JsonCreator
    public TaskEventImpl(@JsonProperty("state") String state)
    {
        super(TopicEventType.TASK, state);
    }

    public TaskEventImpl(TaskEventImpl eventToCopy, String state)
    {
        super(eventToCopy, state);
        this.headers = new HashMap<>(eventToCopy.headers);
        this.lockTime = eventToCopy.lockTime;
        this.lockOwner = eventToCopy.lockOwner;
        this.retries = eventToCopy.retries;
        this.type = eventToCopy.type;
        this.payload.setMsgPack(eventToCopy.payload.getMsgPack());
    }

    @Override
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    @JsonIgnore
    public Instant getLockExpirationTime()
    {
        if (lockTime == Protocol.INSTANT_NULL_VALUE)
        {
            return null;
        }
        else
        {
            return Instant.ofEpochMilli(lockTime);
        }
    }

    public long getLockTime()
    {
        return lockTime;
    }

    public void setLockTime(long lockTime)
    {
        this.lockTime = lockTime;
    }

    @Override
    public Map<String, Object> getHeaders()
    {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers)
    {
        this.headers.clear();
        this.headers.putAll(headers);
    }

    @Override
    public String getLockOwner()
    {
//        if (lockOwner != null && lockOwner.isEmpty())
//        {
//            return null;
//        }
//        else
//        {
        return lockOwner;
//        }
    }

    public void setLockOwner(String lockOwner)
    {
        this.lockOwner = lockOwner;
    }

    @Override
    @JsonIgnore
    public String getPayload()
    {
        return payload.getAsJson();
    }

    @JsonProperty("payload")
    public byte[] getPayloadMsgPack()
    {
        return payload.getMsgPack();
    }

    @JsonProperty("payload")
    public void setPayload(byte[] msgPack)
    {
        this.payload.setMsgPack(msgPack);
    }

    public void setPayload(String json)
    {
        this.payload.setJson(json);
    }

    public void setPayload(InputStream jsonStream)
    {
        this.payload.setJson(jsonStream);
    }

    public Integer getRetries()
    {
        return retries;
    }

    public void setRetries(Integer retries)
    {
        this.retries = retries;
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("TaskEventImpl [state=");
        builder.append(state);
        builder.append(", type=");
        builder.append(type);
        builder.append(", retries=");
        builder.append(retries);
        builder.append(", lockOwner=");
        builder.append(lockOwner);
        builder.append(", lockTime=");
        builder.append(lockTime);
        builder.append(", headers=");
        builder.append(headers);
        builder.append(", payload=");
        builder.append(payload.getAsJson());
        builder.append("]");
        return builder.toString();
    }
}
