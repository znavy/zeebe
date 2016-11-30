package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class EndEventHandler implements BpmnAspectHandler<EndEvent>
{

    @Override
    public void addBehavioralAspects(EndEvent element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {

        aspectsForElement.put(ExecutionEventType.EVT_OCCURRED, BpmnAspect.END_PROCESS);
    }

    @Override
    public Class<EndEvent> getHandledElementType()
    {
        return EndEvent.class;
    }

}
