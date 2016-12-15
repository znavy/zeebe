package org.camunda.tngp.client.impl.cmd;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.io.DirectBufferInputStream;
import org.camunda.tngp.client.ClientCommand;
import org.camunda.tngp.client.cmd.SetPayloadCmd;
import org.camunda.tngp.client.impl.ClientCmdExecutor;
import org.camunda.tngp.client.impl.data.DocumentConverter;
import org.camunda.tngp.util.buffer.PayloadRequestWriter;

@SuppressWarnings("unchecked")
public abstract class AbstractSetPayloadCmd<R, C extends ClientCommand<R>>
    extends AbstractCmdImpl<R> implements SetPayloadCmd<R, C>
{
    protected DocumentConverter documentConverter;

    protected DirectBufferInputStream inStream = new DirectBufferInputStream();
    protected UnsafeBuffer inBuffer = new UnsafeBuffer(0, 0);

    public AbstractSetPayloadCmd(final ClientCmdExecutor cmdExecutor,
            final ClientResponseHandler<R> responseHandler,
            DocumentConverter documentConverter)
    {
        super(cmdExecutor, responseHandler);
        this.documentConverter = documentConverter;
    }

    @Override
    public C payload(String payload)
    {
        return payload(payload.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public C payload(byte[] payload)
    {
        return payload(payload, 0, payload.length);
    }

    @Override
    public C payload(byte[] payload, int offset, int length)
    {
        inBuffer.wrap(payload, offset, length);
        writeCurrentInBuffer();
        return (C) this;
    }

    protected void writeCurrentInBuffer()
    {
        if (inBuffer.capacity() == 0)
        {
            getRequestWriter().write(inBuffer, 0);
            return;
        }
        else
        {
            inStream.wrap(inBuffer);
            final ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);

            try
            {
                documentConverter.convertToMsgPack(inStream, outStream);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Payload is not valid JSON", e);
            }
            final byte[] output = outStream.toByteArray();
            getRequestWriter().payload(output, 0, output.length);
        }

    }

    @Override
    public C payload(ByteBuffer byteBuffer)
    {
        inBuffer.wrap(byteBuffer);
        writeCurrentInBuffer();
        return (C) this;
    }

    @Override
    public C payload(DirectBuffer buffer, int offset, int length)
    {
        inBuffer.wrap(buffer, offset, length);
        writeCurrentInBuffer();
        return (C) this;
    }

    public abstract PayloadRequestWriter getRequestWriter();

}
