package org.camunda.tngp.client.management;

import org.camunda.tngp.client.cmd.ClientCommand;

public interface CreateTopicCmd extends ClientCommand<TopicManagementEvent>
{

    CreateTopicCmd topicName(String topicName);

    CreateTopicCmd partitionCount(int partitionCount);

}
