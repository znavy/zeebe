package org.camunda.tngp.client.task.impl;

import org.camunda.tngp.client.AsyncTasksClient;
import org.camunda.tngp.client.task.Task;

public class TaskImpl implements Task
{
    protected AsyncTasksClient tasksClient;

    protected long id;
    protected String type;
    protected long lockExpirationTime;
    protected int taskQueueId;

    protected int state;
    protected static final int STATE_LOCKED = 0;
    protected static final int STATE_COMPLETED = 1;

    public TaskImpl(AsyncTasksClient tasksClient, long id, String type, long lockExpirationTime, int taskQueueId)
    {
        this.tasksClient = tasksClient;
        this.id = id;
        this.type = type;
        this.lockExpirationTime = lockExpirationTime;
        this.taskQueueId = taskQueueId;
        this.state = STATE_LOCKED;
    }

    @Override
    public void complete()
    {
        tasksClient.complete()
            .taskId(id)
            .taskQueueId(taskQueueId)
            .execute();

        state = STATE_COMPLETED;
    }

    public boolean isCompleted()
    {
        return state == STATE_COMPLETED;
    }

    @Override
    public long getId()
    {
        return id;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public long getLockExpirationTime()
    {
        return lockExpirationTime;
    }

}
