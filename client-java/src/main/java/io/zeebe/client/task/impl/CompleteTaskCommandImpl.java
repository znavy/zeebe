package io.zeebe.client.task.impl;

import java.io.InputStream;

import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.event.impl.EventImpl;
import io.zeebe.client.event.impl.TaskEventImpl;
import io.zeebe.client.impl.ClientCommandManager;
import io.zeebe.client.impl.cmd.CommandImpl;
import io.zeebe.client.task.cmd.CompleteTaskCommand;
import io.zeebe.util.EnsureUtil;

public class CompleteTaskCommandImpl extends CommandImpl<TaskEvent> implements CompleteTaskCommand
{
    protected final TaskEventImpl taskEvent;

    public CompleteTaskCommandImpl(ClientCommandManager client, TaskEventImpl baseEvent)
    {
        super(client);
        EnsureUtil.ensureNotNull("base event", baseEvent);
        this.taskEvent = new TaskEventImpl(baseEvent, TaskEventType.COMPLETE.name());
    }

    @Override
    public EventImpl getEvent()
    {
        return taskEvent;
    }

    @Override
    public String getExpectedStatus()
    {
        return TaskEventType.COMPLETED.name();
    }

    @Override
    public CompleteTaskCommand payload(InputStream payload)
    {
        taskEvent.setPayload(payload);
        return this;
    }

    @Override
    public CompleteTaskCommand payload(String payload)
    {
        taskEvent.setPayload(payload);
        return this;
    }

}
