package org.camunda.tngp.bpmn.graph.validation;

import com.jayway.jsonpath.JsonPath;

public class JaywayJsonPathValidator implements JsonPathValidator
{

    protected JaywayJsonPathValidationResult validationResult = new JaywayJsonPathValidationResult();

    @Override
    public JsonPathValidationResult validate(String jsonPath)
    {
        try
        {
            JsonPath.compile(jsonPath);
            validationResult.isValid = true;
            validationResult.errorMessage = null;
        }
        catch (Exception e)
        {
            validationResult.isValid = false;
            validationResult.errorMessage = e.getMessage();
        }

        return validationResult;
    }

    protected static class JaywayJsonPathValidationResult implements JsonPathValidationResult
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
