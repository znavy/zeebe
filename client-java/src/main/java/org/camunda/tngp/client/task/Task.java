package org.camunda.tngp.client.task;

import java.time.Instant;

import org.agrona.DirectBuffer;


/**
 * Represents a task that was received by a subscription.
 *
 * @author Lindhauer
 */
public interface Task extends WaitStateResponse
{

    /**
     * @return the task's id
     */
    long getId();

    /**
     * @return the id of the workflow instance this task belongs to. May be <code>null</code> if this
     *   is a standalone task.
     */
    Long getWorkflowInstanceId();

    /**
     * @return the task's type
     */
    String getType();

    /**
     * @return the time until when the task is locked
     *   and can be exclusively processed by this client.
     */
    Instant getLockExpirationTime();

    DirectBuffer getPayload();

    String getPayloadString();

    void setPayloadString(String updatedPayload);
}
