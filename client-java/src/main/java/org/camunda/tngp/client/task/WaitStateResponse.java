package org.camunda.tngp.client.task;

public interface WaitStateResponse
{

    /**
     * Triggers completion of this wait state in the broker. Sends no payload.
     */
    void complete();

    /**
     * @param payload must be UTF-8 encoded in case of JSON
     */
    void complete(byte[] payload);

    /**
     * Payload is encoded in UTF-8 before completion.
     */
    void complete(String payload);
}
