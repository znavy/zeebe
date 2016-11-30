package org.camunda.tngp.bpmn.graph.validation;

import java.util.Collection;

import static org.camunda.tngp.bpmn.graph.validation.ValidationCodes.*;

import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.EventDefinition;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;
import org.camunda.bpm.model.bpmn.instance.Process;

public class ProcessStartEventSupportedTypesValidator implements ModelElementValidator<StartEvent>
{

    @Override
    public Class<StartEvent> getElementType()
    {
        return StartEvent.class;
    }

    @Override
    public void validate(StartEvent startEvent, ValidationResultCollector resultCollector)
    {
        final BpmnModelElementInstance scope = startEvent.getScope();

        if (scope instanceof Process)
        {
            final Collection<EventDefinition> eventDefinitions = startEvent.getEventDefinitions();

            if (0 != eventDefinitions.size())
            {
                resultCollector.addError(PROCESS_START_EVENT_UNSUPPORTED, "Usupported process-level start event. Only None Start Events are supported.");
            }
        }
    }

}
