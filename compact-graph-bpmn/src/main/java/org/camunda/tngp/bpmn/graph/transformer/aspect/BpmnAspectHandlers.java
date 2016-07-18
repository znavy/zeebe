package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class BpmnAspectHandlers
{
    protected static final List<BpmnAspectHandler<? extends BaseElement>> HANDLERS = new ArrayList<>();

    static
    {
        HANDLERS.add(new ActivityHandler());
        HANDLERS.add(new EndEventHandler());
        HANDLERS.add(new ProcessHandler());
        HANDLERS.add(new SequenceFlowHandler());
        HANDLERS.add(new StartEventHandler());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map<ExecutionEventType, BpmnAspect> getBehavioralAspects(BaseElement element)
    {

        final Map<ExecutionEventType, BpmnAspect> aspectMap = new HashMap<>();

        for (BpmnAspectHandler<? extends BaseElement> handler : HANDLERS)
        {
            if (handler.getHandledElementType().isAssignableFrom(element.getClass()))
            {
                ((BpmnAspectHandler) handler).addBehavioralAspects(element, aspectMap);
            }
        }

        return aspectMap;
    }

}
