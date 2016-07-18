package org.camunda.tngp.bpmn.graph.transformer.aspect;

import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public interface BpmnAspectHandler<T extends BaseElement>
{
    void addBehavioralAspects(T element, Map<ExecutionEventType, BpmnAspect> aspectsForElement);

    Class<T> getHandledElementType();
}
