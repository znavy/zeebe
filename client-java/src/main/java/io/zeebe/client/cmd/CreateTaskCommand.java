package io.zeebe.client.cmd;

import io.zeebe.client.TopicMetadata;
import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.impl.ZeebeClientImpl;
import io.zeebe.client.impl.task.TaskEventType;
import io.zeebe.protocol.clientapi.EventType;

public class CreateTaskCommand extends Command<TaskEvent>
{
    private final TaskEvent event = new TaskEvent();

    public CreateTaskCommand(ZeebeClientImpl client, String topicName, String type)
    {
        super(client);

        event.setEventType(TaskEventType.CREATE.name());
        event.setType(type);

        final TopicMetadata metadata = new TopicMetadata();
        metadata.setEventType(EventType.TASK_EVENT);
        metadata.setTopicName(topicName);
        // TODO
        metadata.setPartitionId(0);

        event.setTopicMetadata(metadata);
    }

    public CreateTaskCommand retries(int retries)
    {
        event.setRetries(retries);
        return this;
    }

    @Override
    protected TaskEvent getEvent()
    {
        return event;
    }

    @Override
    protected String getExpectedState()
    {
        return TaskEventType.CREATED.name();
    }

}
