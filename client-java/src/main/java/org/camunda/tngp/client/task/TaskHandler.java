package org.camunda.tngp.client.task;

/**
 * Implementations MUST be thread-safe.
 */
@FunctionalInterface
public interface TaskHandler
{
    void handle(Task task);

}
