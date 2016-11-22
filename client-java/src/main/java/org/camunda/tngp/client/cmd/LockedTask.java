package org.camunda.tngp.client.cmd;

import java.time.Instant;

import org.camunda.tngp.client.task.Payload;

public interface LockedTask
{
    long getId();

    Long getWorkflowInstanceId();

    Instant getLockTime();

    Payload getPayload();
}
