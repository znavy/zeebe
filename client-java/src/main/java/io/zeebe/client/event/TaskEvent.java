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
package io.zeebe.client.event;

import java.util.Map;

/**
 * POJO representing an event of type {@link EventType#TASK}.
 */
public class TaskEvent extends Event
{
    private Long lockTime;
    private String lockOwner;
    private Integer retries;
    private String type;
    private Map<String, Object> headers;
    private byte[] payload;

    public TaskEvent()
    {

    }

    public TaskEvent(TaskEvent taskEvent)
    {
        this.lockOwner = taskEvent.getLockOwner();
        // ...
    }

    public void setLockTime(Long lockTime)
    {
        this.lockTime = lockTime;
    }

    public void setLockOwner(String lockOwner)
    {
        this.lockOwner = lockOwner;
    }

    public void setRetries(Integer retries)
    {
        this.retries = retries;
    }

    public void setHeaders(Map<String, Object> headers)
    {
        this.headers = headers;
    }

    public void setPayload(byte[] payload)
    {
        this.payload = payload;
    }

    public Long getLockTime()
    {
        return lockTime;
    }

    public String getLockOwner()
    {
        return lockOwner;
    }

    public Integer getRetries()
    {
        return retries;
    }

    public String getType()
    {
        return type;
    }

    public Map<String, Object> getHeaders()
    {
        return headers;
    }

    public byte[] getPayload()
    {
        return payload;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
