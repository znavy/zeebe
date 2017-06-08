package org.camunda.tngp.client.management;

public interface TopicManagementEvent
{
    String getEventType();

    String getTopicName();

    int getPartitionCount();
}
