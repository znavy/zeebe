package io.zeebe.client;

import io.zeebe.protocol.clientapi.EventType;
import io.zeebe.protocol.clientapi.ExecuteCommandRequestEncoder;

public class TopicMetadata
{
    private String topicName;
    private long key = ExecuteCommandRequestEncoder.keyNullValue();
    private int partitionId = ExecuteCommandRequestEncoder.partitionIdNullValue();
    private EventType entityType;

    public String getTopicName()
    {
        return topicName;
    }

    public void setTopicName(String topicName)
    {
        this.topicName = topicName;
    }

    public long getKey()
    {
        return key;
    }

    public void setKey(long key)
    {
        this.key = key;
    }

    public int getPartitionId()
    {
        return partitionId;
    }

    public void setPartitionId(int partitionId)
    {
        this.partitionId = partitionId;
    }

    public EventType getEventType()
    {
        return entityType;
    }

    public void setEventType(EventType eventType)
    {
        this.entityType = eventType;
    }

}
