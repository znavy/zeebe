package org.camunda.tngp.bpmn.graph.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.camunda.tngp.bpmn.graph.validation.ValidationResultsAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResults;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class DefinitionsSingleProcessValidatorTest
{
    Collection<ModelElementValidator<?>> validators;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        validators = Arrays.asList(new DefinitionsSingleProcessValidator());
    }

    @Test
    public void shouldReportNoProcess()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createEmptyModel();
        final Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace(BPMN20_NS);
        modelInstance.setDefinitions(definitions);

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).hasError(ValidationCodes.DEFINITIONS_NOT_SINGLE_PROCESS);
    }

    @Test
    public void shouldReportTwoProcesses()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createEmptyModel();
        final Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace(BPMN20_NS);
        modelInstance.setDefinitions(definitions);

        final Process process1 = modelInstance.newInstance(Process.class);
        definitions.addChildElement(process1);

        final Process process2 = modelInstance.newInstance(Process.class);
        definitions.addChildElement(process2);

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).hasError(ValidationCodes.DEFINITIONS_NOT_SINGLE_PROCESS);
    }

    @Test
    public void shouldNotReportSingleProcess()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createProcess()
            .startEvent()
            .endEvent()
            .done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(0);
    }

}
