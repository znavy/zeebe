package org.camunda.tngp.client.task.impl;

import java.io.InputStream;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.io.DirectBufferInputStream;
import org.camunda.tngp.client.task.Payload;

public class PayloadImpl implements Payload
{

    protected final byte[] payload = new byte[1024 * 1024]; // TODO: size
    protected final UnsafeBuffer payloadBuffer = new UnsafeBuffer(0, 0);

    public void wrap(DirectBuffer buffer, int offset, int length)
    {
        buffer.getBytes(offset, this.payload, 0, length);
        this.payloadBuffer.wrap(this.payload, 0, length);
    }

    @Override
    public InputStream getRaw()
    {
        return new DirectBufferInputStream(payloadBuffer);
    }

    @Override
    public int rawSize()
    {
        return payloadBuffer.capacity();
    }
}
