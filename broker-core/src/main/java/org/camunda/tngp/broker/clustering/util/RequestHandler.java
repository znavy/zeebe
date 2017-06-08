package org.camunda.tngp.broker.clustering.util;

public interface RequestHandler
{
    void handleRequest();

    default boolean isAsyncWorkComplete()
    {
        return true;
    }

    boolean sendRespone(MessageWriter messageWriter);
}
