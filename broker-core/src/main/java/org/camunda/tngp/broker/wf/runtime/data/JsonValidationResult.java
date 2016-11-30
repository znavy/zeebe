package org.camunda.tngp.broker.wf.runtime.data;

public interface JsonValidationResult
{

    boolean isValid();

    String getErrorMessage();
}
