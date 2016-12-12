package org.camunda.tngp.broker.wf.runtime.data;

public interface MsgPackValidationResult
{

    boolean isValid();

    String getErrorMessage();
}
