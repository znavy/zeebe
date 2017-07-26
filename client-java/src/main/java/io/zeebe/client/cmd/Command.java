package io.zeebe.client.cmd;

import java.util.concurrent.CompletableFuture;

import io.zeebe.client.event.Event;
import io.zeebe.client.impl.ZeebeClientImpl;

public abstract class Command<E extends Event>
{
    private final ZeebeClientImpl client;

    public Command(ZeebeClientImpl client)
    {
        this.client = client;
    }

    public E execute()
    {
        return client.execute(getEvent(), getExpectedState());
    }

    public CompletableFuture<E> executeAsync()
    {
        return client.executeAsync(getEvent(), getExpectedState());
    }

    protected abstract E getEvent();

    protected abstract String getExpectedState();
}
