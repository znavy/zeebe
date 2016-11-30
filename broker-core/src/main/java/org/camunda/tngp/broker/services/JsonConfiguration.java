package org.camunda.tngp.broker.services;

import org.camunda.tngp.broker.wf.runtime.data.JsonDocument;
import org.camunda.tngp.broker.wf.runtime.data.JsonValidator;

public interface JsonConfiguration
{

    JsonDocument buildJsonDocument(int numConcurrentResults);

    JsonValidator buildJsonValidator();
}
