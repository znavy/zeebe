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
            final String conditionArg1 = conditionExpression.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, CAMUNDA_ATTRIBUTE_CONDITION_ARG1);
            populateMsgPackProperty(element.getId(), conditionArg1, elementWriter.conditionArg1());

            final String conditionOperatorValue = conditionExpression.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, CAMUNDA_ATTRIBUTE_CONDITION_OPERATOR);
            elementWriter.conditionOperator(ConditionOperator.valueOf(conditionOperatorValue));

            final String conditionArg2 = conditionExpression.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, CAMUNDA_ATTRIBUTE_CONDITION_ARG2);
            populateMsgPackProperty(element.getId(), conditionArg2, elementWriter.conditionArg2());
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

    protected void populateMsgPackProperty(String bpmnElementId, String value, MsgPackPropertyWriter propertyWriter)
    {
        if (isJsonPathExpression(value))
        {
            // TODO: we could compile the expression alrady here and store the compiled value
            //   => more efficient, but has certain impact on backwards compatibility
            propertyWriter.jsonPathExpression(value);
        }
        else if (isNull(value))
        {
            propertyWriter.nilValue();
        }
        else if (isBoolean(value))
        {
            propertyWriter.value(Boolean.parseBoolean(value));
        }
        else if (isInteger(value))
        {
            propertyWriter.value(Long.parseLong(value));
        }
        else if (isFloat(value))
        {
            propertyWriter.value(Double.parseDouble(value));
        }
        else if (isString(value))
        {
            propertyWriter.value(value.substring(1, value.length() - 1));
        }
        else
        {
            throw new RuntimeException("BPMN element " + bpmnElementId + ": Could not parse condition argument attribute. " +
                    "Must be a JSON path expression, or valid MsgPack String, Boolean, Integer, Float, null constant.");
        }
    }

    public static boolean isInteger(String value)
    {
        try
        {
            Long.parseLong(value);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    public static boolean isFloat(String value)
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

    public static boolean isString(String value)
    {
        return value.startsWith("\"") && value.endsWith("\"");
    }

    public static boolean isBoolean(String value)
    {
        return "true".equals(value) || "false".equals(value);
    }

    public static boolean isNull(String value)
    {
        return "null".equals(value);
    }

    @Override
    public Class<SequenceFlow> getElementType()
    {
        return SequenceFlow.class;
    }

}
