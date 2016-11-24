package org.camunda.tngp.broker.wf.runtime.data;

import java.io.IOException;

import org.agrona.DirectBuffer;
import org.agrona.io.DirectBufferInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;

public class JacksonJsonDocument implements JsonDocument
{
    protected final DirectBufferInputStream inputStream = new DirectBufferInputStream();
    protected final ObjectMapper objectMapper;

    protected JsonNode jsonNode;
    protected JacksonJsonPathResult[] jsonPathResults;
    protected int nextResult;

    /**
     * @param objectMapper
     * @param jsonPathConfiguration
     * @param resultPoolSize the number of {@link JacksonJsonPathResult} instances that can be held in parallel. The <code>resultPoolSize + 1</code>
     *   json path result is going to reuse the first result instance, etc. (round robin)
     */
    public JacksonJsonDocument(ObjectMapper objectMapper, Configuration jsonPathConfiguration, int resultPoolSize)
    {
        this.objectMapper = objectMapper;
        this.jsonPathResults = new JacksonJsonPathResult[resultPoolSize];
        for (int i = 0; i < resultPoolSize; i++)
        {
            jsonPathResults[i] = new JacksonJsonPathResult(jsonPathConfiguration);
        }
        this.nextResult = 0;
    }

    /**
     * @return true if content is valid Jackson; false otherwise; in case false is returned, this method has no
     *   other effect
     */
    public boolean wrap(DirectBuffer buffer, int offset, int length)
    {
        this.inputStream.wrap(buffer, offset, length);
        try
        {
            jsonNode = objectMapper.readTree(inputStream);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    @Override
    public JsonPathResult jsonPath(DirectBuffer jsonPathBuffer, int offset, int length)
    {
        final JacksonJsonPathResult nextResultObject = jsonPathResults[nextResult];
        nextResult = (nextResult + 1) % jsonPathResults.length;

        nextResultObject.wrap(jsonNode, jsonPathBuffer, offset, length);
        return nextResultObject;
    }


}
