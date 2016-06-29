package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class ProcessScopeEventHandler implements BpmnAspectHandler
{

    @Override
    public void addBehavioralAspects(BaseElement element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {
        if (element instanceof Event && element.getParentElement() instanceof Process)
        {
            final Event event = (Event) element;
            if (event.getOutgoing().isEmpty())
            {
                // TODO: support events without outgoing flow
            }

            if (event.getIncoming().isEmpty())
            {
                if (event instanceof StartEvent)
                {
                    aspectsForElement.put(ExecutionEventType.EVT_OCCURRED, BpmnAspect.START_PROCESS);
                }
                else
                {
                    // TODO: boundary events
                }
            }
        }
    }

}
