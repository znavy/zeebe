package org.camunda.tngp.bpmn.graph.validation;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.xml.validation.ModelElementValidator;

public class BpmnExecutionValidators
{
    public static final List<ModelElementValidator<?>> VALIDATORS = new ArrayList<>();

    static
    {
        VALIDATORS.add(new ProcessSingleStartEventValidator());
        VALIDATORS.add(new ProcessStartEventSupportedTypesValidator());
        VALIDATORS.add(new ServiceTaskExtensionValidator());
    }
}
