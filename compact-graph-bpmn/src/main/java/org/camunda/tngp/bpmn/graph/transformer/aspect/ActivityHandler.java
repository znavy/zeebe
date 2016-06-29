package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class ActivityHandler implements BpmnAspectHandler
{

    @Override
    public void addBehavioralAspects(BaseElement element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {
        if (element instanceof Activity)
        {
            aspectsForElement.put(ExecutionEventType.ACT_INST_CREATED, BpmnAspect.NULL_VAL);
        }
    }

}
