package org.camunda.tngp.bpmn.graph.validation;

import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

public class DefinitionsSingleProcessValidator implements ModelElementValidator<Definitions>
{

    @Override
    public Class<Definitions> getElementType()
    {
        return Definitions.class;
    }

    @Override
    public void validate(Definitions element, ValidationResultCollector validationResultCollector)
    {
        if (element.getChildElementsByType(Process.class).size() != 1)
        {
            validationResultCollector.addError(ValidationCodes.DEFINITIONS_NOT_SINGLE_PROCESS, "BPMN XML must contain exactly one process element");
        }
    }

}
