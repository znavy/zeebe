package org.camunda.tngp.broker.wf.runtime.data;

import org.agrona.DirectBuffer;
import org.camunda.tngp.msgpack.query.MsgPackQueryExecutor;
import org.camunda.tngp.msgpack.spec.MsgPackToken;
import org.camunda.tngp.msgpack.spec.MsgPackType;

public class JsonPathResultImpl implements JsonPathResult
{

    protected DirectBuffer documentBuffer;
    protected int numResults;
    protected MsgPackToken resultToken = new MsgPackToken();

    public void wrap(DirectBuffer documentBuffer, MsgPackQueryExecutor jsonPathResultWrapper)
    {
        this.numResults = jsonPathResultWrapper.numResults();
        this.documentBuffer = documentBuffer;

        if (numResults == 1)
        {
            jsonPathResultWrapper.moveToResult(0);
            resultToken.wrap(documentBuffer, jsonPathResultWrapper.currentResultPosition());
        }
    }

    @Override
    public boolean hasResolved()
    {
        return numResults > 0;
    }

    @Override
    public boolean hasSingleResult()
    {
        return numResults == 1;
    }

    @Override
    public boolean isArray()
    {
        return hasResolved() && resultToken.getType() == MsgPackType.ARRAY;
    }

    @Override
    public boolean isString()
    {
        return hasResolved() && resultToken.getType() == MsgPackType.STRING;
    }

    @Override
    public DirectBuffer asEncodedString()
    {
        return resultToken.getValueBuffer();
    }

    @Override
    public boolean isObject()
    {
        return hasResolved() && resultToken.getType() == MsgPackType.MAP;
    }

    @Override
    public boolean isBoolean()
    {
        return hasResolved() && resultToken.getType() == MsgPackType.BOOLEAN;
    }

    @Override
    public boolean asBoolean()
    {
        return resultToken.getBooleanValue();
    }


    @Override
    public boolean isInteger()
    {
        return hasResolved() && resultToken.getType() == MsgPackType.INTEGER;
    }

    @Override
    public long asInteger()
    {
        return resultToken.getIntegerValue();
    }

    @Override
    public boolean isFloat()
    {
        return resultToken.getType() == MsgPackType.FLOAT;
    }

    @Override
    public double asFloat()
    {
        return resultToken.getFloatValue();
    }

    @Override
    public boolean isNil()
    {
        return resultToken.getType() == MsgPackType.NIL;
    }
}