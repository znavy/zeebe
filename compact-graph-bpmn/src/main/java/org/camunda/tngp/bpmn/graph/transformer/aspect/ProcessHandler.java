package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class ProcessHandler implements BpmnAspectHandler
{

    @Override
    public void addBehavioralAspects(BaseElement element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {
        if (element instanceof Process)
        {
            aspectsForElement.put(ExecutionEventType.PROC_INST_CREATED, BpmnAspect.TAKE_INITIAL_FLOWS);
        }

    }

}
