package io.zeebe.client.task.impl.subscription;

import io.zeebe.client.TasksClient;
import io.zeebe.client.event.impl.TaskEventImpl;

// TODO: extract API interface
public class TaskController
{

    protected final TasksClient tasksClient;
    protected final TaskEventImpl baseEvent;

    protected int state;
    protected static final int STATE_LOCKED = 0;
    protected static final int STATE_COMPLETED = 1;
    protected static final int STATE_FAILED = 2;

    public TaskController(TasksClient tasksClient, TaskEventImpl baseEvent)
    {
        this.tasksClient = tasksClient;
        this.baseEvent = baseEvent;
    }

    public void completeTask()
    {
        completeTask(null);
    }

    public void completeTask(String newPayload)
    {
        tasksClient.complete(baseEvent)
            .payload(newPayload)
            .execute();

        state = STATE_COMPLETED;
    }

    public void fail(Exception e)
    {
        tasksClient.fail(baseEvent)
            .retries(baseEvent.getRetries() - 1)
            .execute();

        state = STATE_FAILED;
    }

    public boolean isTaskCompleted()
    {
        return state == STATE_COMPLETED;
    }

    // TODO: in order to make payload and autocompletion work together, we need a setPayload setter (also test this)
}
