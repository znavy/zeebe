package org.camunda.tngp.bpmn.graph.transformer.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

public class BpmnElementTransformers
{
    protected static final Map<Class<? extends BaseElement>, BpmnElementTransformer<? extends BaseElement>> TRANSFORMERS = new HashMap<>();

    static
    {
        addTransformer(new BaseElementTransformer());
        addTransformer(new ServiceTaskTransformer());
        addTransformer(new SequenceFlowTransformer());
    }

    protected static void addTransformer(BpmnElementTransformer<?> transformer)
    {
        TRANSFORMERS.put(transformer.getElementType(), transformer);
    }

    public static void applyTransformers(BaseElement element, FlowElementDescriptorWriter writer)
    {
        final Collection<ModelElementType> extendingTypes = element.getElementType().getAllExtendingTypes();

        // TODO: could also be part of model API
        final List<Class<? extends ModelElementInstance>> implementedTypes = new ArrayList<>();

        ModelElementType currentType = element.getElementType();

        while (currentType.getInstanceType() != BaseElement.class)
        {
            implementedTypes.add(currentType.getInstanceType());
            currentType = currentType.getBaseType();
        }

        implementedTypes.add(BaseElement.class);

        for (int i = implementedTypes.size() - 1; i >= 0; i--)
        {
            final Class<? extends BaseElement> currentClass = (Class<? extends BaseElement>) implementedTypes.get(i);
            final BpmnElementTransformer elementTransformer = TRANSFORMERS.get(currentClass);
            if (elementTransformer != null)
            {
                elementTransformer.transform(element, writer);
            }
        }
    }

}
