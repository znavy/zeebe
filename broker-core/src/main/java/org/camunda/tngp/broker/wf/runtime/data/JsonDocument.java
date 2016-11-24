package org.camunda.tngp.broker.wf.runtime.data;

import org.agrona.DirectBuffer;

public interface JsonDocument
{

    boolean wrap(DirectBuffer buffer, int offset, int length);

    JsonPathResult jsonPath(DirectBuffer jsonPathBuffer, int offset, int length);
}
