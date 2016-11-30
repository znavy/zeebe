package org.camunda.tngp.bpmn.graph.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.bpmn.graph.validation.ValidationResultsAssert.assertThat;
import static org.camunda.tngp.broker.test.util.bpmn.TngpModelInstance.wrap;

import java.util.Arrays;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.ConditionExpression;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
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
    public void shouldValidateConditionsOnRegularSequenceFlow()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance).conditionExpression("flow1", "true", "EQUALS", "true");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).element("flow1").hasError(ValidationCodes.SEQUENCE_FLOW_UNSUPPORTED_CONDITION);
    }

    @Test
    public void shouldValidateConditionOnNonDefaultFlowLeavingGateway()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance)
            .conditionExpression("flow1", "true", "EQUALS", "true")
            .defaultFlow("flow1");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).element("flow1").hasError(ValidationCodes.SEQUENCE_FLOW_BPMN_FORBIDDEN_CONDITION);
    }

    @Test
    public void shouldValidateConditionOnDefaultFlowLeavingGateway()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance)
            .conditionExpression("flow1", "true", "EQUALS", "true")
            .defaultFlow("flow1");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).element("flow1").hasError(ValidationCodes.SEQUENCE_FLOW_BPMN_FORBIDDEN_CONDITION);
    }

    @Test
    public void shouldValidateNoConditionFlowLeavingGateway()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).element("flow1").hasError(ValidationCodes.SEQUENCE_FLOW_MISSING_CONDITION);
    }

    @Test
    public void shouldValidateIncompleteCondition()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();

        final SequenceFlow flow1 = modelInstance.getModelElementById("flow1");
        final ConditionExpression conditionExpression = modelInstance.newInstance(ConditionExpression.class);
        conditionExpression.setAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "arg1", "1");
        flow1.setConditionExpression(conditionExpression);

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(2);
        assertThat(results).element("flow1").hasErrors(
                ValidationCodes.SEQUENCE_FLOW_MISSING_CONDITION_ATTRIBUTE,
                ValidationCodes.SEQUENCE_FLOW_MISSING_CONDITION_ATTRIBUTE);
    }

    @Test
    public void shouldValidateStringLiteral()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance).conditionExpression("flow1", "\"foo\"", "EQUAL", "\"foo");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).element("flow1").hasError(ValidationCodes.SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE);
    }

    @Test
    public void shouldValidateBooleanLiteral()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance).conditionExpression("flow1", "true", "EQUAL", "false");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(0);
    }

    @Test
    public void shouldValidateNullLiteral()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance).conditionExpression("flow1", "null", "EQUAL", "null");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(0);
    }

    @Test
    public void shouldValidateUnknownLiteral()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance).conditionExpression("flow1", "this has no meaning", "EQUAL", "this neither");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(2);
        assertThat(results).element("flow1").hasErrors(
                ValidationCodes.SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE,
                ValidationCodes.SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE);
    }

    @Test
    public void shouldValidateNumberLiteral()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance).conditionExpression("flow1", "1000000", "EQUAL", "123.123123");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(0);

    }

    @Test
    public void shouldValidateOperator()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance).conditionExpression("flow1", "true", "non-existing-operator", "true");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(1);
        assertThat(results).element("flow1").hasErrors(
                ValidationCodes.SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE);
    }

    @Test
    public void shouldValidateIncorrectJsonPath()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance).conditionExpression("flow1", "$....", "EQUAL", "$.[");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(2);
        assertThat(results).element("flow1").hasErrors(
                ValidationCodes.SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE,
                ValidationCodes.SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE);
    }

    @Test
    public void shouldValidateCorrectJsonPath()
    {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn
                    .createExecutableProcess()
                    .startEvent()
                    .exclusiveGateway()
                    .sequenceFlowId("flow1")
                    .endEvent()
                    .done();
        wrap(modelInstance).conditionExpression("flow1", "$.foo", "EQUAL", "$.bar");

        // if
        final ValidationResults results = modelInstance.validate(validators);

        // then
        assertThat(results.getErrorCount()).isEqualTo(0);
    }

}
