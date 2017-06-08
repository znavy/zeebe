package org.camunda.tngp.client.management.impl;

import org.camunda.tngp.client.management.TopicManagementEvent;

public class TopicManagementEventImpl implements TopicManagementEvent
{
    private String eventType;
    private String topicName;
    private int partitionCount;

    @Override
    public String getEventType()
    {
        return eventType;
    }

    @Override
    public int getPartitionCount()
    {
        return partitionCount;
    }

    @Override
    public String getTopicName()
    {
        return topicName;
    }

    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    public void setPartitionCount(int partitionCount)
    {
        this.partitionCount = partitionCount;
    }

    public void setTopicName(String topicName)
    {
        this.topicName = topicName;
    }
}
