package org.camunda.tngp.broker.wf.runtime.data;

import org.agrona.DirectBuffer;

/**
 * Implementations may not be thread-safe.
 */
public interface MsgPackValidator
{

    MsgPackValidationResult validate(DirectBuffer buffer, int offset, int length);
}
