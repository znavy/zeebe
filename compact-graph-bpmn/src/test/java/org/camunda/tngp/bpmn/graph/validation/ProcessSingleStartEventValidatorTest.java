package org.camunda.tngp.bpmn.graph.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.bpmn.graph.validation.ValidationCodes.PROCESS_MULTIPLE_START_EVENTS;
import static org.camunda.tngp.bpmn.graph.validation.ValidationCodes.PROCESS_NO_START_EVENT;
import static org.camunda.tngp.bpmn.graph.validation.ValidationResultsAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResults;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class ProcessSingleStartEventValidatorTest
{
    Collection<ModelElementValidator<?>> validators;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        validators = Arrays.asList(new ProcessSingleStartEventValidator());
    }

    @Test
    public void shouldReportMultipleStartEvents()
    {
        // given
        final ProcessBuilder processBuilder = Bpmn.createExecutableProcess();
        processBuilder.startEvent();
        processBuilder.startEvent();
        final BpmnModelInstance modelInstance = processBuilder.done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).hasError(PROCESS_MULTIPLE_START_EVENTS);
    }

    @Test
    public void shouldReportMissingStartEvent()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess().done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).hasError(PROCESS_NO_START_EVENT);
    }

    @Test
    public void shouldExceptSingleStartEvent()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess().startEvent().done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results).hasNoErrors();
    }

}
