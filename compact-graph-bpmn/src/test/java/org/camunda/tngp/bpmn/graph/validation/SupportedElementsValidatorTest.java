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

public class SupportedElementsValidatorTest
{
    Collection<ModelElementValidator<?>> validators;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        validators = Arrays.asList(new BpmnElementWhitelistValidator());
    }

    @Test
    public void shouldPreventEmbeddedSubProcess()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .subProcess("subProcess")
                    .embeddedSubProcess()
                        .startEvent()
                        .endEvent()
                    .subProcessDone()
                    .endEvent()
                    .done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).element("subProcess").hasError(ValidationCodes.GENERAL_UNSUPPORTED_ELEMENT);
    }
}
