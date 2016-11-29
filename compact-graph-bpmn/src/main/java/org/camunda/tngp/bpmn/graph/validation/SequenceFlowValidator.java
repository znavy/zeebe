package org.camunda.tngp.bpmn.graph.validation;

import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

public class SequenceFlowValidator implements ModelElementValidator<SequenceFlow>
{

    @Override
    public Class<SequenceFlow> getElementType()
    {
        return SequenceFlow.class;
    }

    @Override
    public void validate(SequenceFlow element, ValidationResultCollector validationResultCollector)
    {
        // TODO: validate condition expression
//        if (element.getConditionExpression() != null)
//        {
//            validationResultCollector.addError(ValidationCodes.SEQUENCE_FLOW_UNSUPPORTED_CONDITION, "Sequence flows may not have conditions");
//        }
    }
}
