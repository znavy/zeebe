package org.camunda.tngp.bpmn.graph;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.graph.bpmn.MsgPackType;

public class MsgPackPropertyReader implements MsgPackScalarReader
{

    protected MsgPackType type;
    protected UnsafeBuffer valueBuffer = new UnsafeBuffer(0, 0);

    public void wrap(MsgPackType type, DirectBuffer buffer, int valueOffset, int valueLength)
    {
        this.type = type;

        if (valueLength == 0)
        {
            this.valueBuffer.wrap(0, 0);
        }
        else
        {
            this.valueBuffer.wrap(buffer, valueOffset, valueLength);
        }
    }

    public MsgPackType type()
    {
        return type;
    }

    public DirectBuffer valueExpression()
    {
        return valueBuffer;
    }

    @Override
    public boolean isFloat()
    {
        return type == MsgPackType.FLOAT;
    }

    @Override
    public double asFloat()
    {
        return valueBuffer.getFloat(0);
    }

    @Override
    public boolean isInteger()
    {
        return type == MsgPackType.INTEGER;
    }

    @Override
    public long asInteger()
    {
        return valueBuffer.getLong(0);
    }

    @Override
    public boolean isNil()
    {
        return type == MsgPackType.NIL;
    }

    @Override
    public boolean isString()
    {
        return type == MsgPackType.STRING;
    }

    @Override
    public DirectBuffer asEncodedString()
    {
        return valueBuffer;
    }

    @Override
    public boolean isBoolean()
    {
        return type == MsgPackType.BOOLEAN;
    }

    @Override
    public boolean asBoolean()
    {
        return valueBuffer.getByte(0) != 0;
    }
}
