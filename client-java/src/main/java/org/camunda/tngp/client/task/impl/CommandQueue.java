package org.camunda.tngp.client.task.impl;

public interface CommandQueue<T>
{

    boolean offer(T cmd);

    int drain();
}
