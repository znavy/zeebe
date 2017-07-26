package io.zeebe.client;

import io.zeebe.client.cmd.CreateTaskCommand;
import io.zeebe.client.cmd.TaskCommand;
import io.zeebe.client.event.TaskEvent;

public interface TaskClient
{
    CreateTaskCommand createTask(String topicName, String type);

    TaskCommand completeTask(TaskEvent taskEvent);
}
