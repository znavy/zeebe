package org.camunda.tngp.bpmn.graph.validation;

import java.util.Arrays;
import java.util.Collection;

import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class SequenceFlowValidatorTest
{
    Collection<ModelElementValidator<?>> validators;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        validators = Arrays.asList(new SequenceFlowValidator());
    }

    @Test
    public void shouldValidateSequenceFlowExpression()
    {
        // TODO: test condition expression validation
    }


}
