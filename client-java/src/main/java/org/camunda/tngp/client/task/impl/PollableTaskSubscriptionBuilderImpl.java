package org.camunda.tngp.client.task.impl;

import org.camunda.tngp.client.task.PollableTaskSubscription;
import org.camunda.tngp.client.task.PollableTaskSubscriptionBuilder;

public class PollableTaskSubscriptionBuilderImpl implements PollableTaskSubscriptionBuilder
{

    protected TaskSubscriptionBuilderImpl subscriptionBuilder;

    public PollableTaskSubscriptionBuilderImpl(TaskAcquisition taskAcquisition)
    {
        subscriptionBuilder = new TaskSubscriptionBuilderImpl(taskAcquisition);
    }

    @Override
    public PollableTaskSubscriptionBuilder taskType(String taskType)
    {
        subscriptionBuilder.taskType(taskType);
        return this;
    }

    @Override
    public PollableTaskSubscriptionBuilder lockTime(long lockTime)
    {
        subscriptionBuilder.lockTime(lockTime);
        return this;
    }

    @Override
    public PollableTaskSubscriptionBuilder taskQueueId(int taskQueueId)
    {
        subscriptionBuilder.taskQueueId(taskQueueId);
        return this;
    }

    @Override
    public PollableTaskSubscription open()
    {
        return subscriptionBuilder.open();
    }

}
