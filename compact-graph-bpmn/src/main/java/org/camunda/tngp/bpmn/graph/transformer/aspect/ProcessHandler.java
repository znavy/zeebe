package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class ProcessHandler implements BpmnAspectHandler<Process>
{

    @Override
    public void addBehavioralAspects(Process element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {
        aspectsForElement.put(ExecutionEventType.PROC_INST_CREATED, BpmnAspect.TAKE_OUTGOING_FLOWS);
        aspectsForElement.put(ExecutionEventType.PROC_INST_COMPLETED, BpmnAspect.NULL_VAL);
    }

    @Override
    public Class<Process> getHandledElementType()
    {
        return Process.class;
    }

}
