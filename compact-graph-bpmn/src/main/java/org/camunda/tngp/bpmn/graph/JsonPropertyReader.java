package org.camunda.tngp.bpmn.graph;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.graph.bpmn.JsonType;

public class JsonPropertyReader implements JsonScalarReader
{

    protected JsonType jsonType;
    protected UnsafeBuffer valueBuffer = new UnsafeBuffer(0, 0);

    public void wrap(JsonType jsonType, DirectBuffer buffer, int valueOffset, int valueLength)
    {
        this.jsonType = jsonType;

        if (valueLength == 0)
        {
            this.valueBuffer.wrap(0, 0);
        }
        else
        {
            this.valueBuffer.wrap(buffer, valueOffset, valueLength);
        }
    }

    public JsonType type()
    {
        return jsonType;
    }

    public DirectBuffer valueExpression()
    {
        return valueBuffer;
    }

    @Override
    public boolean isNumber()
    {
        return jsonType == JsonType.NUMBER;
    }

    @Override
    public double asNumber()
    {
        return valueBuffer.getDouble(0);
    }

    @Override
    public boolean isString()
    {
        return jsonType == JsonType.STRING;
    }

    @Override
    public DirectBuffer asEncodedString()
    {
        return valueBuffer;
    }

    @Override
    public boolean isNull()
    {
        return jsonType == JsonType.NULL;
    }

    @Override
    public boolean isBoolean()
    {
        return jsonType == JsonType.BOOLEAN;
    }

    @Override
    public boolean asBoolean()
    {
        return valueBuffer.getByte(0) != 0;
    }
}
