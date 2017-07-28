package io.zeebe.client.task.impl;

import java.util.concurrent.Future;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.zeebe.client.impl.ClientCommandManager;
import io.zeebe.client.impl.Partition;
import io.zeebe.client.task.cmd.Request;
import io.zeebe.protocol.clientapi.ControlMessageType;

public abstract class ControlMessageRequest<R> implements Request<R>
{

    // TODO: metadata wrapper?
    protected final ControlMessageType type;
    protected final Partition target;
    protected final Class<R> responseClass;

    protected final ClientCommandManager client;

    public ControlMessageRequest(ClientCommandManager client, ControlMessageType type, Partition target, Class<R> responseClass)
    {
        this.client = client;
        this.type = type;
        this.target = target;
        this.responseClass = responseClass;
    }

    @JsonIgnore
    public ControlMessageType getType()
    {
        return type;
    }

    @JsonIgnore
    public Partition getTarget()
    {
        return target;
    }

    @JsonIgnore
    public Class<R> getResponseClass()
    {
        return responseClass;
    }

    public abstract Object getRequest();

    @Override
    public R execute()
    {
        return client.execute(this);
    }

    @Override
    public Future<R> executeAsync()
    {
        return client.executeAsync(this);
    }

}
