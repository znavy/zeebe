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

public class SequenceFlowValidatorTest
{
    Collection<ModelElementValidator<?>> validators;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        validators = Arrays.asList(new SequenceFlowValidator());
    }

    @Test
    public void shouldReportUnsupportedStartEvents()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn.createExecutableProcess()
                    .startEvent()
                    .sequenceFlowId("sequenceFlow")
                    .condition("sequenceFlow", "some condition")
                    .done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).element("sequenceFlow").hasError(ValidationCodes.SEQUENCE_FLOW_UNSUPPORTED_CONDITION);
    }
}
