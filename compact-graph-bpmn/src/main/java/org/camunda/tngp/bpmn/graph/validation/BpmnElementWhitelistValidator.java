package org.camunda.tngp.bpmn.graph.validation;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

public class BpmnElementWhitelistValidator implements ModelElementValidator<FlowNode>
{

    protected static final List<Class<? extends FlowNode>> SUPPORTED_FLOW_NODES = new ArrayList<>();

    static
    {
        SUPPORTED_FLOW_NODES.add(StartEvent.class);
        SUPPORTED_FLOW_NODES.add(EndEvent.class);
        SUPPORTED_FLOW_NODES.add(ServiceTask.class);
    }

    @Override
    public Class<FlowNode> getElementType()
    {
        return FlowNode.class;
    }

    @Override
    public void validate(FlowNode element, ValidationResultCollector validationResultCollector)
    {
        final Class<? extends FlowNode> elementType = element.getClass();

        for (int i = 0; i < SUPPORTED_FLOW_NODES.size(); i++)
        {
            if (SUPPORTED_FLOW_NODES.get(i).isAssignableFrom(elementType))
            {
                return;
            }
        }

        validationResultCollector.addError(
                ValidationCodes.GENERAL_UNSUPPORTED_ELEMENT,
                "Element of type " + element.getElementType().getTypeName() + " is not supported for execution");
    }

}
