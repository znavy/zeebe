package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class ActivityHandler implements BpmnAspectHandler<Activity>
{

    @Override
    public void addBehavioralAspects(Activity element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {
        aspectsForElement.put(ExecutionEventType.ACT_INST_CREATED, BpmnAspect.NULL_VAL);

        if (!element.getOutgoing().isEmpty())
        {
            aspectsForElement.put(ExecutionEventType.ACT_INST_COMPLETED, BpmnAspect.TAKE_OUTGOING_FLOWS);
        }
        else
        {
            aspectsForElement.put(ExecutionEventType.ACT_INST_COMPLETED, BpmnAspect.END_PROCESS);
        }
    }

    @Override
    public Class<Activity> getHandledElementType()
    {
        return Activity.class;
    }

}
