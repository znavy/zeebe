package org.camunda.tngp.broker.wf.runtime.data;

import org.agrona.DirectBuffer;

public interface MsgPackDocument
{

    void wrap(DirectBuffer buffer, int offset, int length);

    JsonPathResult jsonPath(DirectBuffer jsonPathBuffer, int offset, int length);
}
