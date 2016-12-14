package org.camunda.tngp.client.task.impl;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.agrona.DirectBuffer;
import org.camunda.tngp.client.AsyncTasksClient;
import org.camunda.tngp.client.cmd.CompleteAsyncTaskCmd;
import org.camunda.tngp.client.impl.data.DocumentConverter;
import org.camunda.tngp.client.task.Payload;
import org.camunda.tngp.client.task.Task;
import org.camunda.tngp.protocol.taskqueue.SubscribedTaskReader;
import org.camunda.tngp.util.EnsureUtil;

public class TaskImpl implements Task
{
    protected final AsyncTasksClient tasksClient;

    protected final long id;
    protected final Long workflowInstanceId;
    protected final String type;
    protected final Instant lockExpirationTime;
    protected final int taskQueueId;
    protected final PayloadImpl payload;

    protected int state;
    protected static final int STATE_LOCKED = 0;
    protected static final int STATE_COMPLETED = 1;


    public TaskImpl(
            AsyncTasksClient tasksClient,
            SubscribedTaskReader taskReader,
            TaskSubscriptionImpl subscription,
            DocumentConverter documentConverter)
    {
        this.tasksClient = tasksClient;
        this.id = taskReader.taskId();
        this.workflowInstanceId = taskReader.wfInstanceId();
        this.type = subscription.getTaskType();
        this.lockExpirationTime = Instant.ofEpochMilli(taskReader.lockTime());
        this.taskQueueId = subscription.getTaskQueueId();
        this.state = STATE_LOCKED;

        this.payload = new PayloadImpl(documentConverter);
        final DirectBuffer payloadBuffer = taskReader.payload();

        final boolean payloadWrapped = payload.wrap(payloadBuffer, 0, payloadBuffer.capacity());
        if (!payloadWrapped)
        {
            throw new RuntimeException("Could not handle payload");
        }
    }

    @Override
    public void complete()
    {
        complete((byte[]) null);
    }

    @Override
    public void complete(String payload)
    {
        // TODO: ensure not null
        EnsureUtil.ensureNotNull("payload", payload);
        complete(payload.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void complete(byte[] payload)
    {
        final CompleteAsyncTaskCmd completeCmd = tasksClient.complete()
            .taskId(id)
            .taskQueueId(taskQueueId);

        if (payload != null)
        {
            completeCmd.payload(payload);
        }

        completeCmd.execute();

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
    public Long getWorkflowInstanceId()
    {
        return workflowInstanceId;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public Instant getLockExpirationTime()
    {
        return lockExpirationTime;
    }

    @Override
    public Payload getPayload()
    {
        return payload;
    }


}
