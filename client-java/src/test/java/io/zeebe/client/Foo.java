package io.zeebe.client;

import io.zeebe.client.event.TaskEvent;

public class Foo
{

    private void main()
    {

        final ZeebeClient zeebeClient = ZeebeClient.createDefaultClient();
        final TaskClient taskClient = zeebeClient.taskClient();

        final TaskEvent createdTask = taskClient.createTask("default", "send-email")
            .execute();

        final TaskEvent completedTask = taskClient.completeTask(createdTask).execute();
    }
}
