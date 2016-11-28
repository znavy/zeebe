package org.camunda.tngp.bpmn.graph.transformer.element;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.ConditionExpression;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.tngp.graph.bpmn.ConditionOperator;

public class SequenceFlowTransformer implements BpmnElementTransformer<SequenceFlow>
{


    public static final String CAMUNDA_ATTRIBUTE_CONDITION_ARG1 = "arg1";
    public static final String CAMUNDA_ATTRIBUTE_CONDITION_ARG2 = "arg2";
    public static final String CAMUNDA_ATTRIBUTE_CONDITION_OPERATOR = "operator";

    @Override
    public void transform(SequenceFlow element, FlowElementDescriptorWriter elementWriter)
    {
        final ConditionExpression conditionExpression = element.getConditionExpression();
        if (conditionExpression != null)
        {
            // TODO: write validator for attribute presence if a condition expression is defined
            final String conditionArg1 = conditionExpression.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, CAMUNDA_ATTRIBUTE_CONDITION_ARG1);
            populateJsonProperty(element.getId(), conditionArg1, elementWriter.conditionArg1());

            final String conditionOperatorValue = conditionExpression.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, CAMUNDA_ATTRIBUTE_CONDITION_OPERATOR);
            elementWriter.conditionOperator(ConditionOperator.valueOf(conditionOperatorValue));

            final String conditionArg2 = conditionExpression.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, CAMUNDA_ATTRIBUTE_CONDITION_ARG2);
            populateJsonProperty(element.getId(), conditionArg2, elementWriter.conditionArg2());
        }

        final FlowNode source = element.getSource();
        if (source instanceof ExclusiveGateway)
        {
            if (((ExclusiveGateway) source).getDefault() == element)
            {
                elementWriter.defaultFlow();
            }
        }
    }

    protected void populateJsonProperty(String bpmnElementId, String value, JsonPropertyWriter propertyWriter)
    {
        if (isJsonPathExpression(value))
        {
            propertyWriter.jsonPathExpression(value);
        }
        else if (isJsonBoolean(value))
        {
            propertyWriter.value(Boolean.parseBoolean(value));
        }
        else if (isJsonNumber(value))
        {
            propertyWriter.value(Double.parseDouble(value));
        }
        else if (isJsonString(value))
        {
            propertyWriter.value(value.substring(1, value.length() - 1));
        }
        else
        {
            // TODO: write validator for condition attributes
            throw new RuntimeException("BPMN element " + bpmnElementId + ": Could not parse attribute " +
                    CAMUNDA_ATTRIBUTE_CONDITION_ARG2 +
                    ". Must be a JSON path expression, JSON String, Boolean or Number constant.");
        }
    }

    public static boolean isJsonNumber(String value)
    {
        try
        {
            Double.parseDouble(value);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    public static boolean isJsonPathExpression(String value)
    {
        return value.startsWith("$");
    }

    public static boolean isJsonString(String value)
    {
        return value.startsWith("\"") && value.endsWith("\"");
    }

    public static boolean isJsonBoolean(String value)
    {
        return "true".equals(value) || "false".equals(value);
    }

    public static boolean isJsonNull(String value)
    {
        return "null".equals(value);
    }

    @Override
    public Class<SequenceFlow> getElementType()
    {
        return SequenceFlow.class;
    }

}
