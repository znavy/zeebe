package org.camunda.tngp.broker.clustering.management.message;

import static org.camunda.tngp.clustering.management.PartitionManagementRequestEncoder.*;
import static org.camunda.tngp.clustering.management.PartitionManagementRequestEncoder.MembersEncoder.*;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.broker.clustering.raft.Member;
import org.camunda.tngp.clustering.management.MessageHeaderDecoder;
import org.camunda.tngp.clustering.management.MessageHeaderEncoder;
import org.camunda.tngp.clustering.management.PartitionManagementOpCode;
import org.camunda.tngp.clustering.management.PartitionManagementRequestDecoder;
import org.camunda.tngp.clustering.management.PartitionManagementRequestDecoder.MembersDecoder;
import org.camunda.tngp.clustering.management.PartitionManagementRequestEncoder;
import org.camunda.tngp.clustering.management.PartitionManagementRequestEncoder.MembersEncoder;
import org.camunda.tngp.transport.SocketAddress;
import org.camunda.tngp.util.buffer.BufferReader;
import org.camunda.tngp.util.buffer.BufferWriter;

public class PartitionManagementRequest implements BufferWriter, BufferReader
{
    protected final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    protected final PartitionManagementRequestDecoder bodyDecoder = new PartitionManagementRequestDecoder();

    protected final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    protected final PartitionManagementRequestEncoder bodyEncoder = new PartitionManagementRequestEncoder();

    protected DirectBuffer topicName = new UnsafeBuffer(0, 0);
    protected int partitionId = partitionIdNullValue();
    protected int term = termNullValue();
    protected List<Member> members = new CopyOnWriteArrayList<>();
    protected PartitionManagementOpCode opCode;

    public PartitionManagementOpCode opCode()
    {
        return opCode;
    }

    public PartitionManagementRequest opCode(PartitionManagementOpCode opCode)
    {
        this.opCode = opCode;
        return this;
    }

    public int partitionId()
    {
        return partitionId;
    }

    public PartitionManagementRequest partitionId(final int partitionId)
    {
        this.partitionId = partitionId;
        return this;
    }

    public DirectBuffer topicName()
    {
        return topicName;
    }

    public PartitionManagementRequest topicName(final DirectBuffer topicName)
    {
        this.topicName.wrap(topicName);
        return this;
    }

    public int term()
    {
        return term;
    }

    public PartitionManagementRequest term(final int term)
    {
        this.term = term;
        return this;
    }

    public List<Member> members()
    {
        return members;
    }

    public PartitionManagementRequest members(final List<Member> members)
    {
        this.members.clear();
        this.members.addAll(members);
        return this;
    }

    @Override
    public int getLength()
    {
        final int size = members.size();

        int length = headerEncoder.encodedLength() + bodyEncoder.sbeBlockLength();

        length += sbeHeaderSize() + (sbeBlockLength() + hostHeaderLength()) * size;

        for (int i = 0; i < size; i++)
        {
            final Member member = members.get(i);
            final SocketAddress endpoint = member.endpoint();
            length += endpoint.hostLength();
        }

        length += topicNameHeaderLength();

        if (topicName != null)
        {
            length += topicName.capacity();
        }

        return length;
    }

    @Override
    public void write(final MutableDirectBuffer buffer, int offset)
    {
        headerEncoder.wrap(buffer, offset)
            .blockLength(bodyEncoder.sbeBlockLength())
            .templateId(bodyEncoder.sbeTemplateId())
            .schemaId(bodyEncoder.sbeSchemaId())
            .version(bodyEncoder.sbeSchemaVersion());

        offset += headerEncoder.encodedLength();

        final int size = members.size();

        final MembersEncoder encoder = bodyEncoder.wrap(buffer, offset)
            .opCode(opCode)
            .partitionId(partitionId)
            .term(term)
            .membersCount(size);

        for (int i = 0; i < size; i++)
        {
            final Member member = members.get(i);
            final SocketAddress endpoint = member.endpoint();

            encoder.next()
                .port(endpoint.port())
                .putHost(endpoint.getHostBuffer(), 0, endpoint.hostLength());
        }

        bodyEncoder.putTopicName(topicName, 0, topicName.capacity());
    }

    @Override
    public void wrap(final DirectBuffer buffer, int offset, final int length)
    {
        final int frameEnd = offset + length;

        headerDecoder.wrap(buffer, offset);
        offset += headerDecoder.encodedLength();

        bodyDecoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

        opCode = bodyDecoder.opCode();
        partitionId = bodyDecoder.partitionId();
        term = bodyDecoder.term();

        members.clear();

        final Iterator<MembersDecoder> iterator = bodyDecoder.members().iterator();

        while (iterator.hasNext())
        {
            final MembersDecoder decoder = iterator.next();

            final Member member = new Member();
            member.endpoint().port(decoder.port());

            final MutableDirectBuffer endpointBuffer = member.endpoint().getHostBuffer();
            final int hostLength = decoder.hostLength();
            member.endpoint().hostLength(hostLength);
            decoder.getHost(endpointBuffer, 0, hostLength);

            members.add(member);
        }

        final int topicNameLength = bodyDecoder.topicNameLength();
        final int topicNameOffset = bodyDecoder.limit() + topicNameHeaderLength();
        topicName.wrap(buffer, topicNameOffset, topicNameLength);

        // skip topic name in decoder
        bodyDecoder.limit(topicNameOffset + topicNameLength);

        assert bodyDecoder.limit() == frameEnd : "Decoder read only to position " + bodyDecoder.limit() + " but expected " + frameEnd + " as final position";
    }

    public void reset()
    {
        topicName.wrap(0, 0);
        opCode = PartitionManagementOpCode.NULL_VAL;
        partitionId = partitionIdNullValue();
        term = termNullValue();
        members.clear();
    }

}
