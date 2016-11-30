package org.camunda.tngp.bpmn.graph.validation;

import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

public class ProcessExecutableValidator implements ModelElementValidator<Process>
{

    @Override
    public Class<Process> getElementType()
    {
        return Process.class;
    }

    @Override
    public void validate(Process element, ValidationResultCollector validationResultCollector)
    {
        if (!element.isExecutable())
        {
            final String errorMessage = String.format("Process %s is not marked executable.", element.getId());
            validationResultCollector.addError(ValidationCodes.PROCESS_NOT_EXECUTABLE, errorMessage);
        }

    }

}
