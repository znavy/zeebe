package org.camunda.tngp.bpmn.graph.transformer.element;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.graph.bpmn.MsgPackType;
import org.camunda.tngp.util.buffer.BufferUtil;

public class MsgPackPropertyWriter
{

    protected byte[] staticBytes = new byte[BitUtil.SIZE_OF_LONG];

    protected MsgPackType type;
    protected UnsafeBuffer propertyBuffer = new UnsafeBuffer(0, 0);


    public MsgPackPropertyWriter()
    {
        reset();
    }

    public MsgPackPropertyWriter value(long scalar)
    {
        propertyBuffer.wrap(staticBytes, 0, BitUtil.SIZE_OF_LONG);
        propertyBuffer.putLong(0, scalar);
        this.type = MsgPackType.INTEGER;
        return this;
    }

    public MsgPackPropertyWriter value(double value)
    {
        propertyBuffer.wrap(staticBytes, 0, BitUtil.SIZE_OF_DOUBLE);
        propertyBuffer.putDouble(0, value);
        this.type = MsgPackType.FLOAT;
        return this;
    }

    public MsgPackPropertyWriter value(String scalar)
    {
        BufferUtil.wrapString(scalar, propertyBuffer);
        this.type = MsgPackType.STRING;
        return this;
    }

    public MsgPackPropertyWriter value(boolean scalar)
    {
        if (scalar)
        {
            staticBytes[0] = 1;
        }
        else
        {
            staticBytes[0] = 0;
        }
        propertyBuffer.wrap(staticBytes, 0, 1);
        this.type = MsgPackType.BOOLEAN;
        return this;
    }

    public MsgPackPropertyWriter nilValue()
    {
        propertyBuffer.wrap(0, 0);
        this.type = MsgPackType.NIL;
        return this;
    }

    public MsgPackPropertyWriter jsonPathExpression(String expression)
    {
        BufferUtil.wrapString(expression, propertyBuffer);
        this.type = MsgPackType.EXPRESSION;
        return this;
    }

    public MsgPackType type()
    {
        return type;
    }

    public DirectBuffer getPropertyBuffer()
    {
        return propertyBuffer;
    }

    public void reset()
    {
        type = MsgPackType.NULL_VAL;
        propertyBuffer.wrap(0, 0);
    }

}
