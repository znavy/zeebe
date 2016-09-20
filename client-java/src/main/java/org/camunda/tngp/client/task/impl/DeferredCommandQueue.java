package org.camunda.tngp.client.task.impl;

import java.util.function.Consumer;

import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;

public class DeferredCommandQueue<T> implements CommandQueue<T>
{

    protected ManyToOneConcurrentArrayQueue<T> cmdQueue;
    protected Consumer<T> consumer;

    public DeferredCommandQueue(Consumer<T> consumer)
    {
        this.consumer = consumer;
        this.cmdQueue = new ManyToOneConcurrentArrayQueue<>(32);
    }


    @Override
    public boolean offer(T cmd)
    {
        return cmdQueue.offer(cmd);
        // TODO: do something if command does not fit in queue
    }

    @Override
    public int drain()
    {
        return cmdQueue.drain(consumer);
    }
}
