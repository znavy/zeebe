package org.camunda.tngp.client.management.impl;

import org.camunda.tngp.client.impl.ClientCmdExecutor;
import org.camunda.tngp.client.impl.cmd.AbstractExecuteCmdImpl;
import org.camunda.tngp.client.management.CreateTopicCmd;
import org.camunda.tngp.client.management.TopicManagementEvent;
import org.camunda.tngp.protocol.clientapi.EventType;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CreateTopicCmdImpl extends AbstractExecuteCmdImpl<TopicManagementEventImpl, TopicManagementEvent> implements CreateTopicCmd
{
    public CreateTopicCmdImpl(ClientCmdExecutor cmdExecutor, ObjectMapper objectMapper)
    {
        super(cmdExecutor, objectMapper, TopicManagementEventImpl.class, "zb-topics-management", 0, EventType.TOPIC_MANAGEMENT_EVENT);
    }

    private TopicManagementEventImpl evt = new TopicManagementEventImpl();
    private String topicName;
    private int partitionCount;

    @Override
    protected TopicManagementEventImpl writeCommand()
    {
        evt.setEventType("CREATE");
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
    public CreateTopicCmd topicName(String topicName)
    {
        this.topicName = topicName;
        return this;
    }

    @Override
    public CreateTopicCmd partitionCount(int partitionCount)
    {
        this.partitionCount = partitionCount;
        return this;
    }



}
