package org.camunda.tngp.client.task.impl;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.client.AsyncTasksClient;
import org.camunda.tngp.client.task.Task;
import org.camunda.tngp.protocol.taskqueue.SubscribedTaskReader;

public class TaskImpl implements Task
{
    protected final AsyncTasksClient tasksClient;

    protected final long id;
    protected final Long workflowInstanceId;
    protected final String type;
    protected final Instant lockExpirationTime;
    protected final int taskQueueId;
    protected final byte[] payload = new byte[1024 * 1024]; // TODO: size
    protected final UnsafeBuffer payloadBuffer = new UnsafeBuffer(0, 0);
    protected String payloadString;

    protected int state;
    protected static final int STATE_LOCKED = 0;
    protected static final int STATE_COMPLETED = 1;

    private byte[] payloadBytes;

    public TaskImpl(
            AsyncTasksClient tasksClient,
            SubscribedTaskReader taskReader,
            TaskSubscriptionImpl subscription)
    {
        this.tasksClient = tasksClient;
        this.id = taskReader.taskId();
        this.workflowInstanceId = taskReader.wfInstanceId();
        this.type = subscription.getTaskType();
        this.lockExpirationTime = Instant.ofEpochMilli(taskReader.lockTime());
        this.taskQueueId = subscription.getTaskQueueId();
        this.state = STATE_LOCKED;
        final DirectBuffer payloadBuffer = taskReader.payload();
        // TODO: check bounds
        payloadBuffer.getBytes(0, this.payload, 0, payloadBuffer.capacity());
        this.payloadBuffer.wrap(this.payload, 0, payloadBuffer.capacity());
        this.payloadString = new String(this.payload, 0, payloadBuffer.capacity(), StandardCharsets.UTF_8);
    }

    @Override
    public void complete()
    {
        tasksClient.complete()
            .taskId(id)
            .taskQueueId(taskQueueId)
            .payload(payloadBuffer, 0, payloadBuffer.capacity())
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
    public DirectBuffer getPayload()
    {
        return payloadBuffer;
    }

    @Override
    public String getPayloadString()
    {
        return payloadString;
    }

    @Override
    public void setPayloadString(String updatedPayload)
    {
        // TODO: ensure not null; ensure length, etc.
        this.payloadString = updatedPayload;
        payloadBytes = updatedPayload.getBytes(StandardCharsets.UTF_8);
        this.payloadBuffer.wrap(payload, 0, payloadBytes.length);
        this.payloadBuffer.putBytes(0, payloadBytes);
    }

}
