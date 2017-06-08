package org.camunda.tngp.broker.clustering.management.message;

import static org.camunda.tngp.clustering.management.PartitionManagementResponseEncoder.termNullValue;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.camunda.tngp.clustering.management.MessageHeaderDecoder;
import org.camunda.tngp.clustering.management.MessageHeaderEncoder;
import org.camunda.tngp.clustering.management.PartitionManagementResponseDecoder;
import org.camunda.tngp.clustering.management.PartitionManagementResponseEncoder;
import org.camunda.tngp.util.buffer.BufferReader;
import org.camunda.tngp.util.buffer.BufferWriter;

public class PartitionManagementResponse implements BufferWriter, BufferReader
{
    protected final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    protected final PartitionManagementResponseDecoder bodyDecoder = new PartitionManagementResponseDecoder();

    protected final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    protected final PartitionManagementResponseEncoder bodyEncoder = new PartitionManagementResponseEncoder();

    protected int term = termNullValue();

    public int term()
    {
        return term;
    }

    public PartitionManagementResponse term(final int term)
    {
        this.term = term;
        return this;
    }

    @Override
    public void wrap(final DirectBuffer buffer, int offset, final int length)
    {
        final int frameEnd = offset + length;

        headerDecoder.wrap(buffer, offset);
        offset += headerDecoder.encodedLength();

        bodyDecoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

        term = bodyDecoder.term();

        assert bodyDecoder.limit() == frameEnd : "Decoder read only to position " + bodyDecoder.limit() + " but expected " + frameEnd + " as final position";
    }

    @Override
    public int getLength()
    {
        return headerEncoder.encodedLength() + bodyEncoder.sbeBlockLength();
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

        bodyEncoder.wrap(buffer, offset)
            .term(term);
    }

    public void reset()
    {
        term = termNullValue();
    }
}
