package org.camunda.tngp.bpmn.graph.validation;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

public class ServiceTaskExtensionValidator implements ModelElementValidator<ServiceTask>
{

    @Override
    public Class<ServiceTask> getElementType()
    {
        return ServiceTask.class;
    }

    @Override
    public void validate(ServiceTask serviceTask, ValidationResultCollector validationCollector)
    {
        final String taskType = serviceTask.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "taskType");
        if (taskType == null)
        {
            validationCollector.addError(ValidationCodes.SERVICE_TASK_MISSING_TASK_TYPE, "Attribute camunda:taskType is missing");
        }

        final String taskQueueIdAttribute = serviceTask.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "taskQueueId");

        if (taskQueueIdAttribute == null)
        {
            validationCollector.addError(ValidationCodes.SERVICE_TASK_MISSING_TASK_QUEUE_ID, "Attribute camunda:taskQueueId is missing");
        }
        else
        {
            try
            {
                Short.parseShort(taskQueueIdAttribute);
            }
            catch (NumberFormatException e)
            {
                validationCollector.addError(ValidationCodes.SERVICE_TASK_INVALID_TASK_QUEUE_ID, "Attribute camunda:taskQueueId must have a valid numerical value in the short range");
            }

        }

    }

}
