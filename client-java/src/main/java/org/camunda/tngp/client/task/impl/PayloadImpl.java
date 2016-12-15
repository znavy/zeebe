package org.camunda.tngp.client.task.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.io.DirectBufferInputStream;
import org.camunda.tngp.client.impl.data.DocumentConverter;
import org.camunda.tngp.client.task.Payload;

public class PayloadImpl implements Payload
{

    protected DocumentConverter documentConverter;

    protected final UnsafeBuffer jsonBuffer = new UnsafeBuffer(0, 0);

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
            final ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);

            try
            {
                documentConverter.convertToJson(inputStream, outStream);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }

            final byte[] json = outStream.toByteArray();
            jsonBuffer.wrap(json, 0, json.length);
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
