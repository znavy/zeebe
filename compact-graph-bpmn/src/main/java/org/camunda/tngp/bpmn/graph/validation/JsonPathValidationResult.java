package org.camunda.tngp.bpmn.graph.validation;

public interface JsonPathValidationResult
{
    boolean isValid();

    String getErrorMessage();
}
