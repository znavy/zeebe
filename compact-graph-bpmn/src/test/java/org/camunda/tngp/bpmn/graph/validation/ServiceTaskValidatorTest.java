package org.camunda.tngp.bpmn.graph.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.bpmn.graph.validation.ValidationResultsAssert.assertThat;
import static org.camunda.tngp.broker.test.util.bpmn.TngpModelInstance.wrap;

import java.util.Arrays;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResults;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class ServiceTaskValidatorTest
{

    Collection<ModelElementValidator<?>> validators;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        validators = Arrays.asList(new ServiceTaskExtensionValidator());
    }

    @Test
    public void shouldReportServiceTaskWithoutTaskType()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .serviceTask("serviceTask")
                    .endEvent()
                    .done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(2);
        assertThat(results).element("serviceTask").hasError(ValidationCodes.SERVICE_TASK_MISSING_TASK_TYPE);
        assertThat(results).element("serviceTask").hasError(ValidationCodes.SERVICE_TASK_MISSING_TASK_QUEUE_ID);
    }

    @Test
    public void shouldReportInvalidTaskQueueId()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .serviceTask("serviceTask")
                    .endEvent()
                    .done();

        wrap(modelInstance).taskAttributes("serviceTask", "foo", 123);
        modelInstance
            .getModelElementById("serviceTask")
            .setAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "taskQueueId", "thisIsNotValid");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).element("serviceTask").hasError(ValidationCodes.SERVICE_TASK_INVALID_TASK_QUEUE_ID);
    }
}
