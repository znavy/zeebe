package org.camunda.tngp.bpmn.graph;

import org.agrona.DirectBuffer;

// TODO: should go to util
public interface JsonScalarReader
{

    boolean isNumber();

    double asNumber();

    boolean isString();

    DirectBuffer asEncodedString();

    boolean isNull();

    boolean isBoolean();

    boolean asBoolean();
}
