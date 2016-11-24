package org.camunda.tngp.broker.wf.runtime.data;

import org.camunda.tngp.bpmn.graph.JsonScalarReader;

public interface JsonPathResult extends JsonScalarReader
{

    boolean hasResolved();

    boolean isArray();

    boolean isObject();
}
