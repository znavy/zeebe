package org.camunda.tngp.client.task;

public interface PollableTaskSubscriptionBuilder
{

    PollableTaskSubscriptionBuilder taskType(String taskType);

    PollableTaskSubscriptionBuilder lockTime(long lockTime);

    PollableTaskSubscriptionBuilder taskQueueId(int taskQueueId);

    PollableTaskSubscription open();
}
