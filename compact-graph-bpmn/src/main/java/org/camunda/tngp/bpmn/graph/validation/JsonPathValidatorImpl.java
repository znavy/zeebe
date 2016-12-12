package org.camunda.tngp.bpmn.graph.validation;

import org.camunda.tngp.msgpack.jsonpath.JsonPathQuery;
import org.camunda.tngp.msgpack.jsonpath.JsonPathQueryCompiler;

public class JsonPathValidatorImpl implements JsonPathValidator
{

    protected JsonPathQueryCompiler queryCompiler = new JsonPathQueryCompiler();
    protected JsonPathValidationResultImpl result = new JsonPathValidationResultImpl();

    @Override
    public JsonPathValidationResult validate(String jsonPath)
    {
        final JsonPathQuery jsonPathQuery = queryCompiler.compile(jsonPath);
        result.isValid = jsonPathQuery.isValid();
        if (!result.isValid)
        {
            result.errorMessage = "Invalid json-path expression. Compilation error at position " +
                    jsonPathQuery.getInvalidPosition() + ": " +
                    jsonPathQuery.getErrorReason();
        }

        return result;
    }

    protected class JsonPathValidationResultImpl implements JsonPathValidationResult
    {

        protected boolean isValid;
        protected String errorMessage;

        @Override
        public boolean isValid()
        {
            return isValid;
        }

        @Override
        public String getErrorMessage()
        {
            return errorMessage;
        }

    }

}
