package org.camunda.tngp.broker.wf.runtime.data;

import org.camunda.tngp.bpmn.graph.MsgPackScalarReader;

public interface JsonPathResult extends MsgPackScalarReader
{

    boolean hasResolved();

    boolean hasSingleResult();

    boolean isArray();

    boolean isObject();
}
