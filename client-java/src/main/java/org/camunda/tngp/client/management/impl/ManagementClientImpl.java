package org.camunda.tngp.client.management.impl;

import org.camunda.tngp.client.ManagementClient;
import org.camunda.tngp.client.impl.ClientCmdExecutor;
import org.camunda.tngp.client.management.CreateTopicCmd;
import org.camunda.tngp.client.management.DeleteTopicCmd;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ManagementClientImpl implements ManagementClient
{
    private ClientCmdExecutor cmdExecutor;
    private ObjectMapper objectMapper;

    public ManagementClientImpl(ClientCmdExecutor cmdExecutor, ObjectMapper objectMapper)
    {
        this.cmdExecutor = cmdExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    public CreateTopicCmd createTopic()
    {
        return new CreateTopicCmdImpl(cmdExecutor, objectMapper);
    }

    @Override
    public DeleteTopicCmd deleteTopic()
    {
        return new DeleteTopicCmdImpl(cmdExecutor, objectMapper);
    }

}
