package io.zeebe.client.cmd;

import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.impl.ZeebeClientImpl;
import io.zeebe.client.impl.task.TaskEventType;

public class CompleteTaskCommand extends Command<TaskEvent>
{
    private final TaskEvent event;

    public CompleteTaskCommand(ZeebeClientImpl client, TaskEvent taskEvent)
    {
        super(client);
        this.event = taskEvent;
    }

    @Override
    protected TaskEvent getEvent()
    {
        return event;
    }

    // TODO set payload

    @Override
    protected String getExpectedState()
    {
        return TaskEventType.COMPLETED.name();
    }
}
