package io.zeebe.client.task.impl;

import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.event.impl.EventImpl;
import io.zeebe.client.event.impl.TaskEventImpl;
import io.zeebe.client.impl.ClientCommandManager;
import io.zeebe.client.impl.cmd.CommandImpl;
import io.zeebe.client.task.cmd.UpdateTaskRetriesCommand;
import io.zeebe.util.EnsureUtil;

public class UpdateRetriesCommandImpl extends CommandImpl<TaskEvent> implements UpdateTaskRetriesCommand
{

    protected final TaskEventImpl taskEvent;

    public UpdateRetriesCommandImpl(ClientCommandManager client, TaskEvent baseEvent)
    {
        super(client);
        EnsureUtil.ensureNotNull("base event", baseEvent);
        this.taskEvent = new TaskEventImpl((TaskEventImpl) baseEvent, TaskEventType.UPDATE_RETRIES.name());
    }

    @Override
    public UpdateTaskRetriesCommand retries(int remainingRetries)
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
        return TaskEventType.RETRIES_UPDATED.name();
    }

}
