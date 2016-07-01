package org.camunda.tngp.bpmn.graph.validation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.validation.ValidationResult;
import org.camunda.bpm.model.xml.validation.ValidationResults;

public class ValidationResultsAssert extends AbstractAssert<ValidationResultsAssert, ValidationResults>
{
    protected BpmnModelElementInstance element;

    protected ValidationResultsAssert(ValidationResults actual)
    {
        super(actual, ValidationResultsAssert.class);
    }

    public static ValidationResultsAssert assertThat(ValidationResults results)
    {
        return new ValidationResultsAssert(results);
    }

    public ValidationResultsAssert element(BpmnModelElementInstance element)
    {
        this.element = element;
        return this;
    }

    public ValidationResultsAssert element(String elementId)
    {
        final Optional<ModelElementInstance> element = actual.getResults()
                .keySet()
                .stream()
                .filter((e) -> elementId.equals(((BaseElement)e).getId()))
                .findFirst();

        if (element.isPresent())
        {
            element((BpmnModelElementInstance) element.get());
        }
        else
        {
            failWithMessage("No validation results reported for element with id <%s>.", elementId);
        }
        return this;
    }

    public ValidationResultsAssert hasError(int expectedCode)
    {
        final List<ValidationResult> resultList;

        if (element == null)
        {
            resultList = actual.getResults().values().stream()
                    .flatMap(l -> l.stream())
                    .collect(Collectors.toList());
        }
        else
        {
            resultList = actual.getResults().get(element);
        }

        final boolean hasError = resultList.stream()
                .anyMatch((v) -> expectedCode == v.getCode());

        if (!hasError)
        {
            final String actualCodes = resultList.stream()
                .map((o) -> String.valueOf(o.getCode()))
                .collect(Collectors.joining(","));

            failWithMessage("Expected validation to have provided error with code <%s> but got <%s>", expectedCode, actualCodes);
        }

        return this;
    }

    public void hasNoErrors()
    {
        if (actual.hasErrors())
        {
            failWithMessage("Expected result to contain no errors. Got <%s> errors.", actual.getErrorCount());
        }
    }

}
