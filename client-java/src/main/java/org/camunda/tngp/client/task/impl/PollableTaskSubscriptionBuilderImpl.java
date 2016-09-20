package org.camunda.tngp.client.task.impl;

import java.util.concurrent.TimeUnit;

import org.camunda.tngp.client.task.PollableTaskSubscription;
import org.camunda.tngp.client.task.PollableTaskSubscriptionBuilder;
import org.camunda.tngp.util.EnsureUtil;

public class PollableTaskSubscriptionBuilderImpl implements PollableTaskSubscriptionBuilder
{

    protected String taskType;
    protected long lockTime = TimeUnit.MINUTES.toMillis(1);
    protected int taskQueueId = 0;

    protected TaskAcquisition taskAcquisition;

    public PollableTaskSubscriptionBuilderImpl(TaskAcquisition taskAcquisition)
    {
        this.taskAcquisition = taskAcquisition;
    }

    @Override
    public PollableTaskSubscriptionBuilder taskType(String taskType)
    {
        this.taskType = taskType;
        return this;
    }

    @Override
    public PollableTaskSubscriptionBuilder lockTime(long lockTime)
    {
        this.lockTime = lockTime;
        return this;
    }

    @Override
    public PollableTaskSubscriptionBuilder taskQueueId(int taskQueueId)
    {
        this.taskQueueId = taskQueueId;
        return this;
    }

    @Override
    public PollableTaskSubscription open()
    {
        EnsureUtil.ensureNotNull("taskType", taskType);
        EnsureUtil.ensureGreaterThan("lockTime", lockTime, 0L);

        final TaskSubscriptionImpl subscription =
                new TaskSubscriptionImpl(taskType, taskQueueId, lockTime, 1, taskAcquisition);
        subscription.open();
        return subscription;
    }
}
