package org.camunda.tngp.broker.wf.runtime.data;

import org.agrona.DirectBuffer;
import org.agrona.io.DirectBufferInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonJsonValidator implements JsonValidator
{

    protected final ObjectMapper objectMapper;
    protected final JacksonJsonValidationResult result = new JacksonJsonValidationResult();
    protected final DirectBufferInputStream inputStream = new DirectBufferInputStream();

    public JacksonJsonValidator(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonValidationResult validate(DirectBuffer buffer, int offset, int length)
    {
        inputStream.wrap(buffer, offset, length);
        try
        {
            objectMapper.readTree(inputStream);
            result.isValid = true;
            result.errorMessage = null;
        }
        catch (Exception e)
        {
            result.isValid = false;
            result.errorMessage = e.getMessage();
        }

        return result;
    }

    protected static class JacksonJsonValidationResult implements JsonValidationResult
    {

        protected boolean isValid = true;
        protected String errorMessage = null;

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
