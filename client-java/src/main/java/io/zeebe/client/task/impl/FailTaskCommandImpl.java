package io.zeebe.client.task.impl;

import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.event.impl.EventImpl;
import io.zeebe.client.event.impl.TaskEventImpl;
import io.zeebe.client.impl.ClientCommandManager;
import io.zeebe.client.impl.cmd.CommandImpl;
import io.zeebe.client.task.cmd.FailTaskCommand;
import io.zeebe.util.EnsureUtil;

public class FailTaskCommandImpl extends CommandImpl<TaskEvent> implements FailTaskCommand
{

    protected final TaskEventImpl taskEvent;

    public FailTaskCommandImpl(ClientCommandManager client, TaskEvent baseEvent)
    {
        super(client);
        EnsureUtil.ensureNotNull("base event", baseEvent);
        this.taskEvent = new TaskEventImpl((TaskEventImpl) baseEvent, TaskEventType.FAIL.name());
    }

    @Override
    public FailTaskCommand retries(int remainingRetries)
    {
        this.taskEvent.setRetries(remainingRetries);
        return this;
    }

    @Override
    public EventImpl getEvent()
    {
        return taskEvent;
    }

    @Override
    public String getExpectedStatus()
    {
        return TaskEventType.FAILED.name();
    }

}
