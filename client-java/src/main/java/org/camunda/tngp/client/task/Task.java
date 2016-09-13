package org.camunda.tngp.client.task;

public interface Task extends WaitStateResponse
{

    long getId();

    String getType();

    long getLockExpirationTime();
}
