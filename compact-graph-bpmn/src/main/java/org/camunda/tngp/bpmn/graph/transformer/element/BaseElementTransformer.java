package org.camunda.tngp.bpmn.graph.transformer.element;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.camunda.tngp.bpmn.graph.transformer.FlowElementTypeMapping;
import org.camunda.tngp.graph.bpmn.FlowElementType;

public class BaseElementTransformer implements BpmnElementTransformer<BaseElement>
{

    @Override
    public void transform(BaseElement element, FlowElementDescriptorWriter elementWriter)
    {
        final ModelElementType elementType = element.getElementType();
        final Class<? extends ModelElementInstance> instanceType = elementType.getInstanceType();
        final String id = element.getId();

        final FlowElementType flowElementType = FlowElementTypeMapping.graphNodeTypeForModelType(instanceType);

        // defaults
        elementWriter.flowElementType(flowElementType);
        elementWriter.id(id);
    }

    @Override
    public Class<BaseElement> getElementType()
    {
        return BaseElement.class;
    }

}
