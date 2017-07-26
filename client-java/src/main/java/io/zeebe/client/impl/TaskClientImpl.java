package io.zeebe.client.impl;

import io.zeebe.client.TaskClient;
import io.zeebe.client.cmd.CreateTaskCommand;
import io.zeebe.client.cmd.TaskCommand;
import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.impl.task.TaskEventType;

public class TaskClientImpl implements TaskClient
{
    private final ZeebeClientImpl client;

    public TaskClientImpl(ZeebeClientImpl client)
    {
        this.client = client;
    }

    @Override
    public CreateTaskCommand createTask(String topicName, String type)
    {
        return new CreateTaskCommand(client, topicName, type);
    }

    @Override
    public TaskCommand completeTask(TaskEvent sourceEvent)
    {
        final TaskEvent taskEvent = new TaskEvent(sourceEvent);
        taskEvent.setEventType(TaskEventType.COMPLETE);

        return new TaskCommand(client, taskEvent);
    }

}
