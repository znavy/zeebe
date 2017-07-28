package io.zeebe.client.impl;

import org.agrona.DirectBuffer;

import io.zeebe.client.clustering.impl.ClientTopologyManager;
import io.zeebe.protocol.clientapi.MessageHeaderDecoder;
import io.zeebe.transport.RemoteAddress;
import io.zeebe.util.buffer.BufferWriter;

public interface RequestResponseHandler extends BufferWriter
{

    boolean handlesResponse(MessageHeaderDecoder responseHeader);

    Object getResult(DirectBuffer buffer, int offset, int blockLength, int version);

    RemoteAddress getTarget(ClientTopologyManager currentTopology);

    String describeRequest();

}
