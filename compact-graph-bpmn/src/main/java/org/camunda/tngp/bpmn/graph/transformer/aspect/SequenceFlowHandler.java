package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class SequenceFlowHandler implements BpmnAspectHandler
{

    @Override
    public void addBehavioralAspects(BaseElement element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {
        if (element instanceof SequenceFlow)
        {
            aspectsForElement.put(ExecutionEventType.SQF_EXECUTED, BpmnAspect.CREATE_ACTIVITY_INSTANCE);
        }

    }

}
