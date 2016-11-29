package org.camunda.tngp.bpmn.graph.transformer.element;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

public class ServiceTaskTransformer implements BpmnElementTransformer<ServiceTask>
{

    @Override
    public void transform(ServiceTask element, FlowElementDescriptorWriter elementWriter)
    {
        final String taskQueueIdAttribute = element.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "taskQueueId");
        final short taskQueueId = Short.parseShort(taskQueueIdAttribute);

        final String taskType = element.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "taskType");

        elementWriter.taskType(taskType);
        elementWriter.taskQueueId(taskQueueId);
    }

    @Override
    public Class<ServiceTask> getElementType()
    {
        return ServiceTask.class;
    }

}
