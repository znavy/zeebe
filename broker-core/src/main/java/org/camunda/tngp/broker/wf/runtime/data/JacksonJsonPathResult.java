package org.camunda.tngp.broker.wf.runtime.data;

import java.nio.charset.StandardCharsets;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class JacksonJsonPathResult implements JsonPathResult
{

    protected final Configuration jsonPathConfiguration;

    protected JsonNode resultNode;
    protected UnsafeBuffer stringResultBuffer = new UnsafeBuffer(0, 0);

    public JacksonJsonPathResult(Configuration jsonPathConfiguration)
    {
        this.jsonPathConfiguration = jsonPathConfiguration;
    }

    public void wrap(JsonNode jsonNode, DirectBuffer jsonPathBuffer, int offset, int length)
    {
        final DocumentContext documentContext = JsonPath.parse(jsonNode, jsonPathConfiguration);

        final byte[] jsonPathBytes = new byte[length]; // could cache a single byte array, yet JSON-path parsing is garbage-laden anyway, so no big gain
        jsonPathBuffer.getBytes(offset, jsonPathBytes, 0, length);
        final String jsonPathString = new String(jsonPathBytes, StandardCharsets.UTF_8);

        try
        {
            resultNode = documentContext.read(jsonPathString);
        }
        catch (Exception e)
        {
            resultNode = null;
        }
    }

    @Override
    public boolean hasResolved()
    {
        return resultNode != null;
    }

    @Override
    public boolean isNumber()
    {
        return hasResolved() && resultNode.isNumber();
    }

    @Override
    public double asNumber()
    {
        return resultNode.doubleValue();
    }

    @Override
    public boolean isArray()
    {
        return hasResolved() && resultNode.isArray();
    }

    @Override
    public boolean isString()
    {
        return hasResolved() && resultNode.isTextual();
    }

    @Override
    public DirectBuffer asEncodedString()
    {
        wrapString(resultNode.textValue(), stringResultBuffer);
        return stringResultBuffer;
    }

    @Override
    public boolean isObject()
    {
        return hasResolved() && resultNode.isObject();
    }

    @Override
    public boolean isNull()
    {
        return hasResolved() && resultNode.isNull();
    }

    @Override
    public boolean isBoolean()
    {
        return hasResolved() && resultNode.isBoolean();
    }

    @Override
    public boolean asBoolean()
    {
        return resultNode.booleanValue();
    }

    // TODO: move to util
    protected void wrapString(String argument, UnsafeBuffer buffer)
    {
        final byte[] bytes = argument.getBytes(StandardCharsets.UTF_8);
        buffer.wrap(bytes);

    }
}