package io.zeebe.client.clustering.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.client.clustering.RequestTopologyCmd;
import io.zeebe.client.impl.ClientCommandManager;
import io.zeebe.client.impl.cmd.AbstractControlMessageCmd;
import io.zeebe.protocol.clientapi.ControlMessageType;
import io.zeebe.util.buffer.BufferWriter;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

public class RequestTopologyCmdImpl extends AbstractControlMessageCmd<TopologyResponse, TopologyResponse> implements RequestTopologyCmd
{
    public RequestTopologyCmdImpl(ClientCommandManager commandManager, final ObjectMapper objectMapper)
    {
        super(commandManager, objectMapper, null, TopologyResponse.class, ControlMessageType.REQUEST_TOPOLOGY);
    }

    @Override
    protected Object writeCommand()
    {
        return null;
    }

    @Override
    protected void reset()
    {
    }

    @Override
    protected TopologyResponse getResponseValue(final TopologyResponse topology)
    {
        return topology;
    }

    @Override
    protected void validate()
    {

    }

    public BufferWriter getRequestWriter()
    {
        final ExpandableArrayBuffer writeBuffer = new ExpandableArrayBuffer();

        writeCommand(writeBuffer);

        return new BufferWriter()
        {
            @Override
            public void write(MutableDirectBuffer buffer, int offset)
            {
                buffer.putBytes(offset, writeBuffer, 0, writeBuffer.capacity());
            }

            @Override
            public int getLength()
            {
                return writeBuffer.capacity();
            }
        };
    }
}
