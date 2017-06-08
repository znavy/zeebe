package org.camunda.tngp.broker.clustering.util;

import static org.camunda.tngp.transport.protocol.Protocols.REQUEST_RESPONSE;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.camunda.tngp.dispatcher.FragmentHandler;
import org.camunda.tngp.transport.protocol.TransportHeaderDescriptor;
import org.camunda.tngp.transport.requestresponse.RequestResponseProtocolHeaderDescriptor;

public class RequestData implements FragmentHandler
{
    private final TransportHeaderDescriptor requestTransportHeaderDescriptor = new TransportHeaderDescriptor();
    private final RequestResponseProtocolHeaderDescriptor requestResponseProtocolHeaderDescriptor = new RequestResponseProtocolHeaderDescriptor();

    protected final MutableDirectBuffer msgBuffer = new ExpandableArrayBuffer();
    protected int msgLength;

    protected int channelId;
    protected long requestId;
    protected long connectionId;

    public void reset()
    {
        if (msgLength > 0)
        {
            msgBuffer.setMemory(0, msgBuffer.capacity(), (byte) 0);
            msgLength = -1;
            channelId = -1;
            connectionId = -1L;
            requestId = -1L;
        }
    }

    @Override
    public int onFragment(DirectBuffer buffer, int offset, int length, int streamId, boolean isMarkedFailed)
    {
        int messageOffset = offset + TransportHeaderDescriptor.headerLength();
        int messageLength = length - TransportHeaderDescriptor.headerLength();

        requestTransportHeaderDescriptor.wrap(buffer, offset);

        channelId = streamId;

        final int protocol = requestTransportHeaderDescriptor.protocolId();

        if (protocol == REQUEST_RESPONSE)
        {
            requestResponseProtocolHeaderDescriptor.wrap(buffer, messageOffset);

            connectionId = requestResponseProtocolHeaderDescriptor.connectionId();
            requestId = requestResponseProtocolHeaderDescriptor.requestId();

            messageOffset += RequestResponseProtocolHeaderDescriptor.headerLength();
            messageLength -= RequestResponseProtocolHeaderDescriptor.headerLength();
        }

        msgBuffer.putBytes(0, buffer, messageOffset, messageLength);

        this.msgLength = messageLength;

        return CONSUME_FRAGMENT_RESULT;
    }

    public DirectBuffer getMsgBuffer()
    {
        return msgBuffer;
    }

    public int getMsgLength()
    {
        return msgLength;
    }

    public int getChannelId()
    {
        return channelId;
    }

    public long getConnectionId()
    {
        return connectionId;
    }

    public long getRequestId()
    {
        return requestId;
    }
}

