package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class StartEventHandler implements BpmnAspectHandler<StartEvent>
{

    @Override
    public void addBehavioralAspects(StartEvent element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {
        aspectsForElement.put(ExecutionEventType.EVT_OCCURRED, BpmnAspect.START_PROCESS);
    }

    @Override
    public Class<StartEvent> getHandledElementType()
    {
        return StartEvent.class;
    }

}
