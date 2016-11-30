package org.camunda.tngp.bpmn.graph.validation;

import java.io.StringWriter;
import java.util.Formatter;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.camunda.bpm.model.xml.validation.ValidationResult;
import org.camunda.bpm.model.xml.validation.ValidationResultFormatter;

@SuppressWarnings("resource")
public class BpmnValidationResultFormatter implements ValidationResultFormatter
{

    @Override
    public void formatElement(StringWriter stringWriter, ModelElementInstance modelElementInstance)
    {
        final ModelElementType elementType = modelElementInstance.getElementType();
        final String elementTypeName = elementType.getTypeName();

        String name = null;
        String id = null;

        if (modelElementInstance instanceof BaseElement)
        {
            id = ((BaseElement) modelElementInstance).getId();
        }

        if (modelElementInstance.getDomElement().hasAttribute("name"))
        {
            name = modelElementInstance.getDomElement().getAttribute("name");
        }

        new Formatter(stringWriter)
            .format("<%s> (%s, %s):\n", elementTypeName, name, id)
            .flush();
    }


    @Override
    public void formatResult(StringWriter stringWriter, ValidationResult result)
    {
        new Formatter(stringWriter)
            .format("\t%s %d: %s\n", result.getType(), result.getCode(), result.getMessage())
            .flush();
    }

}
