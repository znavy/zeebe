package org.camunda.tngp.bpmn.graph.transformer.element;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.graph.bpmn.JsonType;
import org.camunda.tngp.util.buffer.BufferUtil;

public class JsonPropertyWriter
{

    protected JsonType jsonType;
    protected UnsafeBuffer propertyBuffer = new UnsafeBuffer(0, 0);


    public JsonPropertyWriter()
    {
        reset();
    }

    public JsonPropertyWriter value(Number scalar)
    {
        // TODO: double is probably not a good idea
        final byte[] bytes = new byte[8];
        final double value = scalar.doubleValue();

        propertyBuffer.wrap(bytes);
        propertyBuffer.putDouble(0, value);
        this.jsonType = JsonType.NUMBER;
        return this;
    }

    public JsonPropertyWriter value(String scalar)
    {
        BufferUtil.wrapString(scalar, propertyBuffer);
        this.jsonType = JsonType.STRING;
        return this;
    }

    public JsonPropertyWriter value(boolean scalar)
    {
        if (scalar)
        {
            propertyBuffer.wrap(new byte[]{1});
        }
        else
        {
            propertyBuffer.wrap(new byte[]{0});
        }
        this.jsonType = JsonType.BOOLEAN;
        return this;
    }

    public JsonPropertyWriter jsonPathExpression(String expression)
    {
        BufferUtil.wrapString(expression, propertyBuffer);
        this.jsonType = JsonType.EXPRESSION;
        return this;
    }

    public JsonType jsonType()
    {
        return jsonType;
    }

    public DirectBuffer getPropertyBuffer()
    {
        return propertyBuffer;
    }

    public void reset()
    {
        jsonType = JsonType.NULL_VAL;
        propertyBuffer.wrap(0, 0);
    }

}
