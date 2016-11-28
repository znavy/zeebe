package org.camunda.tngp.bpmn.graph.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.agrona.collections.Int2IntHashMap;
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
        hasErrors(expectedCode);

        return this;
    }

    public ValidationResultsAssert hasErrors(int... expectedErrorCodes)
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

        final Int2IntHashMap expectedErrorCodeOccurrences = Arrays.stream(expectedErrorCodes).collect(
            () -> new Int2IntHashMap(0),
            (map, i) -> map.put(i, map.get(i) + 1),
            (map, map2) ->
            { });


        final Int2IntHashMap actualErrorCodeOccurrences = resultList.stream().mapToInt((r) -> r.getCode()).collect(
            () -> new Int2IntHashMap(0),
            (map, i) -> map.put(i, map.get(i) + 1),
            (map, map2) ->
            { });


        final boolean mismatching = expectedErrorCodeOccurrences.keySet().stream()
            .filter((code) -> expectedErrorCodeOccurrences.get(code) != actualErrorCodeOccurrences.get(code))
            .findAny()
            .isPresent();

        if (mismatching)
        {
            final String expectedErrorCodesString = Arrays.stream(expectedErrorCodes)
                .mapToObj((i) -> Integer.toString(i))
                .collect(Collectors.joining(","));
            final String actualCodes = resultList.stream()
                .map((o) -> String.valueOf(o.getCode()))
                .collect(Collectors.joining(","));

            failWithMessage("Expected validation to have provided errors with codes <%s> but got <%s>", expectedErrorCodesString, actualCodes);
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
