package io.zeebe.client.event.impl;

import io.zeebe.client.event.EventMetadata;
import io.zeebe.client.event.TopicEventType;
import io.zeebe.protocol.clientapi.ExecuteCommandRequestEncoder;

public class EventMetadataImpl implements EventMetadata
{

    protected String topicName;
    protected int partitionId = ExecuteCommandRequestEncoder.partitionIdNullValue();
    protected long key = ExecuteCommandRequestEncoder.keyNullValue();
    protected long position = -1;
    protected TopicEventType eventType;

    @Override
    public String getTopicName()
    {
        return topicName;
    }

    public void setTopicName(String topicName)
    {
        this.topicName = topicName;
    }

    @Override
    public int getPartitionId()
    {
        return partitionId;
    }

    public void setPartitionId(int partitionId)
    {
        this.partitionId = partitionId;
    }

    // TODO: this is not available in command requests and responses
    @Override
    public long getEventPosition()
    {
        return position;
    }

    public void setEventPosition(long position)
    {
        this.position = position;
    }

    @Override
    public long getEventKey()
    {
        return key;
    }

    public void setEventKey(long key)
    {
        this.key = key;
    }

    @Override
    public TopicEventType getEventType()
    {
        return eventType;
    }

    public void setEventType(TopicEventType eventType)
    {
        this.eventType = eventType;
    }

    @Override
    public String toString()
    {
        return "EventMetadata [topicName=" + topicName + ", partitionId=" + partitionId + ", key=" +
                key + ", position=" + position + ", eventType=" + eventType + "]";
    }

}
