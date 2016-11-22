package org.camunda.tngp.client.impl.cmd;

import java.time.Instant;

import org.agrona.DirectBuffer;
import org.camunda.tngp.client.cmd.LockedTask;
import org.camunda.tngp.client.task.Payload;
import org.camunda.tngp.client.task.impl.PayloadImpl;

public class LockedTaskImpl implements LockedTask
{
    protected long id;
    protected Long workflowInstanceId;
    protected Instant lockTime;
    protected PayloadImpl payload = new PayloadImpl();

    public void setId(long taskId)
    {
        this.id = taskId;

    }

    @Override
    public long getId()
    {
        return id;
    }

    public void setLockTime(Instant lockTime)
    {
        this.lockTime = lockTime;
    }

    @Override
    public Instant getLockTime()
    {
        return lockTime;
    }

    @Override
    public Long getWorkflowInstanceId()
    {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(Long workflowInstanceId)
    {
        this.workflowInstanceId = workflowInstanceId;
    }

    @Override
    public Payload getPayload()
    {
        return this.payload;
    }


    public void setPayload(DirectBuffer buffer, int offset, int length)
    {
        this.payload.wrap(buffer, offset, length);
    }
}
