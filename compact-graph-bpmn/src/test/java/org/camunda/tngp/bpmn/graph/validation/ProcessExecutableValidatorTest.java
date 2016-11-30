package org.camunda.tngp.bpmn.graph.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.bpmn.graph.validation.ValidationResultsAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResults;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class ProcessExecutableValidatorTest
{
    Collection<ModelElementValidator<?>> validators;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        validators = Arrays.asList(new ProcessExecutableValidator());
    }

    @Test
    public void shouldReportNonExecutableProcess()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createProcess()
            .startEvent()
            .endEvent()
            .done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).hasError(ValidationCodes.PROCESS_NOT_EXECUTABLE);
    }

    @Test
    public void shouldNotReportExecutableProcess()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess()
            .startEvent()
            .endEvent()
            .done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(0);
    }

}
