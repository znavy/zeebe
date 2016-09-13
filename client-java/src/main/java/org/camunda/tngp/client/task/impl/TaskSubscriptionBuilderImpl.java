package org.camunda.tngp.client.task.impl;

import java.util.concurrent.TimeUnit;

import org.camunda.tngp.client.task.TaskHandler;
import org.camunda.tngp.client.task.TaskSubscriptionBuilder;

public class TaskSubscriptionBuilderImpl implements TaskSubscriptionBuilder
{

    protected String topic;
    protected long lockTime = TimeUnit.MINUTES.toMillis(1);
    protected int taskQueueId = 0;
    protected TaskHandler taskHandler;

    protected TaskAcquisition taskAcquisition;

    public TaskSubscriptionBuilderImpl(TaskAcquisition taskAcquisition)
    {
        this.taskAcquisition = taskAcquisition;
    }

    @Override
    public TaskSubscriptionBuilder taskType(String topic)
    {
        this.topic = topic;
        return this;
    }

    @Override
    public TaskSubscriptionBuilder lockTime(long lockTime)
    {
        this.lockTime = lockTime;
        return this;
    }

    @Override
    public TaskSubscriptionBuilder taskQueueId(int taskQueueId)
    {
        this.taskQueueId = taskQueueId;
        return this;
    }

    @Override
    public TaskSubscriptionBuilder handler(TaskHandler handler)
    {
        this.taskHandler = handler;
        return this;
    }

    @Override
    public TaskSubscriptionImpl open()
    {
        // TODO: ensure parameters have valid values
        final TaskSubscriptionImpl subscription =
                new TaskSubscriptionImpl(taskHandler, topic, taskQueueId, lockTime, 1, taskAcquisition);
        subscription.open();
        return subscription;
    }


}
