package org.camunda.tngp.bpmn.graph.validation;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.ConditionExpression;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;
import org.camunda.tngp.bpmn.graph.transformer.element.SequenceFlowTransformer;
import org.camunda.tngp.graph.bpmn.ConditionOperator;

public class SequenceFlowValidator implements ModelElementValidator<SequenceFlow>
{

    protected JsonPathValidator jsonPathValidator = new JaywayJsonPathValidator();

    @Override
    public Class<SequenceFlow> getElementType()
    {
        return SequenceFlow.class;
    }

    @Override
    public void validate(SequenceFlow element, ValidationResultCollector validationResultCollector)
    {
        if (element.getSource() instanceof ExclusiveGateway)
        {
            validateLeavingGateway(element, validationResultCollector);
        }
        else
        {
            validateNonGateway(element, validationResultCollector);
        }
    }

    protected void validateNonGateway(SequenceFlow element, ValidationResultCollector validationResultCollector)
    {
        if (element.getConditionExpression() != null)
        {
            validationResultCollector.addError(ValidationCodes.SEQUENCE_FLOW_UNSUPPORTED_CONDITION,
                    "Condition expressions are only supported for flows leaving exclusive gateways");
        }
    }

    protected void validateLeavingGateway(SequenceFlow element, ValidationResultCollector validationResultCollector)
    {
        final ExclusiveGateway gateway = (ExclusiveGateway) element.getSource();
        if (gateway.getDefault() == element)
        {
            validateDefaultFlow(element, validationResultCollector);
        }
        else
        {
            final ConditionExpression conditionExpression = element.getConditionExpression();

            if (conditionExpression != null)
            {
                validateConditionExpression(conditionExpression, validationResultCollector);
            }
            else
            {
                if (gateway.getOutgoing().size() > 1)
                {
                    validationResultCollector.addError(ValidationCodes.SEQUENCE_FLOW_MISSING_CONDITION,
                            "With more than one outgoing sequence flow leaving gateway " + gateway.getId() +
                            ", a condition expression is required");
                }
            }
        }

    }

    private void validateConditionExpression(ConditionExpression conditionExpression, ValidationResultCollector validationResultCollector)
    {
        final String arg1 = conditionExpression.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, SequenceFlowTransformer.CAMUNDA_ATTRIBUTE_CONDITION_ARG1);
        if (validateConditionExpressionAttributeNotNull(
                SequenceFlowTransformer.CAMUNDA_ATTRIBUTE_CONDITION_ARG1,
                arg1,
                validationResultCollector))
        {
            validateConditionArgument(SequenceFlowTransformer.CAMUNDA_ATTRIBUTE_CONDITION_ARG1,
                    arg1,
                    validationResultCollector);
        }

        final String arg2 = conditionExpression.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, SequenceFlowTransformer.CAMUNDA_ATTRIBUTE_CONDITION_ARG2);
        if (validateConditionExpressionAttributeNotNull(
                SequenceFlowTransformer.CAMUNDA_ATTRIBUTE_CONDITION_ARG2,
                arg2,
                validationResultCollector))
        {
            validateConditionArgument(SequenceFlowTransformer.CAMUNDA_ATTRIBUTE_CONDITION_ARG2,
                    arg2,
                    validationResultCollector);
        }

        final String operator = conditionExpression.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, SequenceFlowTransformer.CAMUNDA_ATTRIBUTE_CONDITION_OPERATOR);
        if (validateConditionExpressionAttributeNotNull(
                SequenceFlowTransformer.CAMUNDA_ATTRIBUTE_CONDITION_OPERATOR,
                operator,
                validationResultCollector))
        {
            if (!isValidComparisonOperator(operator))
            {
                validationResultCollector.addError(
                        ValidationCodes.SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE,
                        "Condition expression attribute " + SequenceFlowTransformer.CAMUNDA_ATTRIBUTE_CONDITION_OPERATOR + " has invalid value.");
            }
        }
    }

    protected static boolean isValidComparisonOperator(String operator)
    {
        try
        {
            ConditionOperator.valueOf(operator);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private void validateDefaultFlow(SequenceFlow element, ValidationResultCollector validationResultCollector)
    {
        if (element.getConditionExpression() != null)
        {
            validationResultCollector.addError(ValidationCodes.SEQUENCE_FLOW_BPMN_FORBIDDEN_CONDITION,
                    "Default flows should not have a condition expression");
        }
    }

    protected boolean validateConditionExpressionAttributeNotNull(String attributeName,
            String attributeValue, ValidationResultCollector validationResultCollector)
    {
        if (attributeValue == null)
        {
            validationResultCollector.addError(ValidationCodes.SEQUENCE_FLOW_MISSING_CONDITION_ATTRIBUTE,
                    "Condition expression attribute " + attributeName + " is not present");
            return false;
        }
        else
        {
            return true;
        }
    }

    protected void validateConditionArgument(String attributeName, String attributeValue,
            ValidationResultCollector validationResultCollector)
    {
        if (!(SequenceFlowTransformer.isJsonBoolean(attributeValue) ||
                SequenceFlowTransformer.isJsonNull(attributeValue) ||
                SequenceFlowTransformer.isJsonNumber(attributeValue) ||
                SequenceFlowTransformer.isJsonString(attributeValue)))
        {
            if (SequenceFlowTransformer.isJsonPathExpression(attributeValue))
            {
                final JsonPathValidationResult validationResult = jsonPathValidator.validate(attributeValue);
                if (!validationResult.isValid())
                {
                    validationResultCollector.addError(ValidationCodes.SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE,
                            "Condition expression attribute " + attributeName + " has invalid JSON path value: " +
                            validationResult.getErrorMessage());
                }
            }
            else
            {
                validationResultCollector.addError(ValidationCodes.SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE,
                        "Condition expression attribute " + attributeName + " has invalid value. " +
                        "Must be a Json path expression or a JSON String/Number/Boolean/Null literal.");
            }
        }

    }

}
