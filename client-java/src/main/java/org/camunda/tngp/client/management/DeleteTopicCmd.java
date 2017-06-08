package org.camunda.tngp.client.management;

import org.camunda.tngp.client.cmd.ClientCommand;

public interface DeleteTopicCmd extends ClientCommand<TopicManagementEvent>
{

    DeleteTopicCmd topicName(String topicName);

    DeleteTopicCmd partitionCount(int partitionCount);

}
