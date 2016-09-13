package org.camunda.tngp.client.task;

public interface TaskSubscriptionBuilder
{

    TaskSubscriptionBuilder taskType(String taskType);

    TaskSubscriptionBuilder lockTime(long lockTime);

    TaskSubscriptionBuilder taskQueueId(int taskQueueId);

    TaskSubscriptionBuilder handler(TaskHandler handler);

    TaskSubscription open();
}
