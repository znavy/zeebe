package io.zeebe.client.impl.cmd;

import java.util.concurrent.Future;

import io.zeebe.client.event.Event;
import io.zeebe.client.event.impl.EventImpl;
import io.zeebe.client.impl.ClientCommandManager;
import io.zeebe.client.task.cmd.Request;

public abstract class CommandImpl<E extends Event> implements Request<E>
{

    // TODO: or whoever has the smallest possible interface for running commands
    protected final ClientCommandManager client;

    public CommandImpl(ClientCommandManager client)
    {
        this.client = client;
    }

    @Override
    public E execute()
    {
        return client.execute(this);
    }

    @Override
    public Future<E> executeAsync()
    {
        return client.executeAsync(this);
    }

    public String generateError(E requestEvent, E responseEvent)
    {
        final long requestEventKey = requestEvent.getMetadata().getEventKey();
        final StringBuilder sb = new StringBuilder();
        sb.append("Command ");

        if (requestEventKey >= 0)
        {
            sb.append("for event with key ");
            sb.append(requestEventKey);
            sb.append(" ");
        }

        sb.append("was rejected by broker (");
        sb.append(responseEvent.getState());
        sb.append(")");

        return sb.toString();
    }

    public abstract EventImpl getEvent();

    public abstract String getExpectedStatus();
}
