package org.camunda.tngp.broker.clustering.util;

public interface AsyncRequestProcessor
{
    RequestHandler selectHandler(RequestData data);
}
