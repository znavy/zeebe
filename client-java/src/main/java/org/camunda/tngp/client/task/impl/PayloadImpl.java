package org.camunda.tngp.client.task.impl;

import java.io.InputStream;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.io.DirectBufferInputStream;
import org.agrona.io.DirectBufferOutputStream;
import org.camunda.tngp.client.impl.data.DocumentConverter;
import org.camunda.tngp.client.task.Payload;

public class PayloadImpl implements Payload
{

    protected DocumentConverter documentConverter;

    protected final byte[] jsonPayload = new byte[1024 * 1024]; // TODO: size
    protected final UnsafeBuffer jsonBuffer = new UnsafeBuffer(0, 0);
    protected final DirectBufferOutputStream jsonOutputStream = new DirectBufferOutputStream();

    protected DirectBufferInputStream inputStream = new DirectBufferInputStream();

    public PayloadImpl(DocumentConverter documentConverter)
    {
        this.documentConverter = documentConverter;
    }

    public boolean wrap(DirectBuffer msgPackBuffer, int offset, int length)
    {

        if (length > 0)
        {
            inputStream.wrap(msgPackBuffer, offset, length);
            jsonBuffer.wrap(jsonPayload);
            jsonOutputStream.wrap(jsonBuffer);

            try
            {
                documentConverter.convertToJson(inputStream, jsonOutputStream);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }

            jsonBuffer.wrap(jsonPayload, 0, jsonOutputStream.position());
            return true;
        }
        else
        {
            jsonBuffer.wrap(0, 0);
            return true;
        }
    }

    @Override
    public InputStream getRaw()
    {
        return new DirectBufferInputStream(jsonBuffer);
    }

    @Override
    public int rawSize()
    {
        return jsonBuffer.capacity();
    }

}
