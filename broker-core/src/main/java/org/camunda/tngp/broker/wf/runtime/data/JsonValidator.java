package org.camunda.tngp.broker.wf.runtime.data;

import org.agrona.DirectBuffer;

/**
 * Implementations may not be thread-safe.
 */
public interface JsonValidator
{

    JsonValidationResult validate(DirectBuffer buffer, int offset, int length);
}
