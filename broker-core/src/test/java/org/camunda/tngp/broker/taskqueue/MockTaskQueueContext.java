package org.camunda.tngp.broker.taskqueue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.camunda.tngp.broker.log.LogWriter;
import org.camunda.tngp.broker.wf.runtime.data.JsonValidationResult;
import org.camunda.tngp.broker.wf.runtime.data.JsonValidator;
import org.camunda.tngp.log.Log;
import org.camunda.tngp.log.idgenerator.impl.PrivateIdGenerator;

public class MockTaskQueueContext extends TaskQueueContext
{
    public MockTaskQueueContext()
    {
        super(null, 0);

        setLog(mock(Log.class));
        setLogWriter(mock(LogWriter.class));
        setTaskInstanceIdGenerator(new PrivateIdGenerator(0L));

        jsonValidator = mock(JsonValidator.class);
        final JsonValidationResult validationResult = mock(JsonValidationResult.class);
        when(validationResult.isValid()).thenReturn(true);
        when(jsonValidator.validate(any(), anyInt(), anyInt())).thenReturn(validationResult);
    }

}
