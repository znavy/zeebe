package org.camunda.tngp.bpmn.graph.transformer.element;

import org.camunda.bpm.model.bpmn.instance.BaseElement;

public interface BpmnElementTransformer<T extends BaseElement>
{

    void transform(T element, FlowElementDescriptorWriter elementWriter);

    Class<T> getElementType();

}
