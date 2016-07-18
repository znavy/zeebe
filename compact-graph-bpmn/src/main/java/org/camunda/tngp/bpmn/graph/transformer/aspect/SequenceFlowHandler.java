package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class SequenceFlowHandler implements BpmnAspectHandler<SequenceFlow>
{

    @Override
    public void addBehavioralAspects(SequenceFlow element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {
        final FlowNode target = element.getTarget();
        if (target instanceof Activity)
        {
            aspectsForElement.put(ExecutionEventType.SQF_EXECUTED, BpmnAspect.CREATE_ACTIVITY_INSTANCE);
        }
        else if (target instanceof Event)
        {
            aspectsForElement.put(ExecutionEventType.SQF_EXECUTED, BpmnAspect.TRIGGER_NONE_EVENT);
        }
    }

    @Override
    public Class<SequenceFlow> getHandledElementType()
    {
        return SequenceFlow.class;
    }

}
