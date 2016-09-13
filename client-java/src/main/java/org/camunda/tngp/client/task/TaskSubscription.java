package org.camunda.tngp.client.task;

public interface TaskSubscription
{

    boolean isOpen();

    void close();
}
