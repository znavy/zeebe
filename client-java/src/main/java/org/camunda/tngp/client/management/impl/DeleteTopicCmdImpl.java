package org.camunda.tngp.client.management.impl;

import org.camunda.tngp.client.impl.ClientCmdExecutor;
import org.camunda.tngp.client.impl.cmd.AbstractExecuteCmdImpl;
import org.camunda.tngp.client.management.DeleteTopicCmd;
import org.camunda.tngp.client.management.TopicManagementEvent;
import org.camunda.tngp.protocol.clientapi.EventType;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DeleteTopicCmdImpl extends AbstractExecuteCmdImpl<TopicManagementEventImpl, TopicManagementEvent> implements DeleteTopicCmd
{
    public DeleteTopicCmdImpl(ClientCmdExecutor cmdExecutor, ObjectMapper objectMapper)
    {
        super(cmdExecutor, objectMapper, TopicManagementEventImpl.class, "zb-topics-management", 0, EventType.TOPIC_MANAGEMENT_EVENT);
    }

    private TopicManagementEventImpl evt = new TopicManagementEventImpl();
    private String topicName;
    private int partitionCount;

    @Override
    protected TopicManagementEventImpl writeCommand()
    {
        evt.setEventType("DELETE");
        evt.setPartitionCount(partitionCount);
        evt.setTopicName(topicName);
        return evt;
    }

    @Override
    protected long getKey()
    {
        return -1L;
    }

    @Override
    protected void reset()
    {
        evt = new TopicManagementEventImpl();
    }

    @Override
    protected TopicManagementEvent getResponseValue(int channelId, long key, TopicManagementEventImpl event)
    {
        return event;
    }

    @Override
    public DeleteTopicCmd topicName(String topicName)
    {
        this.topicName = topicName;
        return this;
    }

    @Override
    public DeleteTopicCmd partitionCount(int partitionCount)
    {
        this.partitionCount = partitionCount;
        return this;
    }



}
