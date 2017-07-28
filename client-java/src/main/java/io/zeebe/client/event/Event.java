package io.zeebe.client.event;

public interface Event
{

    EventMetadata getMetadata();

    /**
     * @return the name of the state in the event's lifecycle. The lifecycle is different for each type
     *   of event.
     */
    String getState();
}
