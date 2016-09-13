package org.camunda.tngp.client.task;

public interface PollableTaskSubscription
{

    boolean isOpen();

    void close();

    int poll(TaskHandler taskHandler);
}
