package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Collection;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class ExclusiveGatewayHandler implements BpmnAspectHandler<ExclusiveGateway>
{

    @Override
    public void addBehavioralAspects(ExclusiveGateway element, Map<ExecutionEventType, BpmnAspect> aspectsForElement)
    {
        final Collection<SequenceFlow> outgoingFlows = element.getOutgoing();

        if (outgoingFlows.isEmpty())
        {
            aspectsForElement.put(ExecutionEventType.GW_ACTIVATED, BpmnAspect.END_PROCESS);
        }
        else if (outgoingFlows.size() == 1)
        {
            final SequenceFlow flow = outgoingFlows.iterator().next();
            if (flow.getConditionExpression() != null)
            {
                aspectsForElement.put(ExecutionEventType.GW_ACTIVATED, BpmnAspect.EXCLUSIVE_SPLIT);
            }
            else
            {
                aspectsForElement.put(ExecutionEventType.GW_ACTIVATED, BpmnAspect.TAKE_OUTGOING_FLOWS);
            }
        }
        else
        {
            aspectsForElement.put(ExecutionEventType.GW_ACTIVATED, BpmnAspect.EXCLUSIVE_SPLIT);
        }
    }

    @Override
    public Class<ExclusiveGateway> getHandledElementType()
    {
        return ExclusiveGateway.class;
    }

}
