package org.camunda.tngp.client;

import org.camunda.tngp.client.management.CreateTopicCmd;
import org.camunda.tngp.client.management.DeleteTopicCmd;

public interface ManagementClient
{
    CreateTopicCmd createTopic();

    DeleteTopicCmd deleteTopic();
}
