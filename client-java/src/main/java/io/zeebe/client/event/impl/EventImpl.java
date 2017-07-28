package io.zeebe.client.event.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.zeebe.client.event.Event;
import io.zeebe.client.event.EventMetadata;
import io.zeebe.client.event.TopicEventType;

public abstract class EventImpl implements Event
{
    protected final EventMetadataImpl metadata = new EventMetadataImpl();
    protected final String state;

    public EventImpl(TopicEventType type, String state)
    {
        this.metadata.setEventType(type);
        this.state = state;
    }

    public EventImpl(EventImpl baseEvent, String state)
    {
        updateMetadata(baseEvent.metadata);
        this.state = state;
    }

    @Override
    @JsonIgnore
    public EventMetadata getMetadata()
    {
        return metadata;
    }

    public void setTopicName(String name)
    {
        this.metadata.setTopicName(name);
    }

    public void setPartitionId(int id)
    {
        this.metadata.setPartitionId(id);
    }

    public void setKey(long key)
    {
        this.metadata.setEventKey(key);
    }

    public void setEventPosition(long position)
    {
        this.metadata.setEventPosition(position);
    }

    public void updateMetadata(EventMetadata other)
    {
        this.metadata.setEventKey(other.getEventKey());
        this.metadata.setEventPosition(other.getEventPosition());
        this.metadata.setEventType(other.getEventType());
        this.metadata.setPartitionId(other.getPartitionId());
        this.metadata.setTopicName(other.getTopicName());
    }

    @Override
    public String getState()
    {
        return state;
    }

}
