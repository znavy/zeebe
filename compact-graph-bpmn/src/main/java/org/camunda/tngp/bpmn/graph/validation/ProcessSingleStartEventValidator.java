package org.camunda.tngp.bpmn.graph.validation;

import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

import static org.camunda.tngp.bpmn.graph.validation.ValidationCodes.*;

import java.util.Collection;

import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

public class ProcessSingleStartEventValidator implements ModelElementValidator<Process>
{

    @Override
    public Class<Process> getElementType()
    {
        return Process.class;
    }

    @Override
    public void validate(Process process, ValidationResultCollector resultCollector)
    {
        final Collection<StartEvent> startEvents = process.getChildElementsByType(StartEvent.class);

        if (startEvents.size() > 1)
        {
            final String errorMessage = String.format("Must have exactly one start event. Got %d.", startEvents.size());
            resultCollector.addError(PROCESS_MULTIPLE_START_EVENTS, errorMessage);
        }
        else if (startEvents.size() == 0)
        {
            final String errorMessage = String.format("Must have a start event. No start event found.");
            resultCollector.addError(PROCESS_NO_START_EVENT, errorMessage);
        }
    }

}
