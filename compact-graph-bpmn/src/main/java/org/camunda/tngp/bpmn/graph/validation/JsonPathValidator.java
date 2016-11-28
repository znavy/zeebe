package org.camunda.tngp.bpmn.graph.validation;

public interface JsonPathValidator
{
    JsonPathValidationResult validate(String jsonPath);
}
