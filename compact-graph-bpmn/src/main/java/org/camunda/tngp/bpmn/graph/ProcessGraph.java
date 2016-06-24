package org.camunda.tngp.bpmn.graph;

import org.camunda.tngp.compactgraph.Graph;
import org.camunda.tngp.graph.bpmn.ProcessDescriptorDecoder;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

public class ProcessGraph extends Graph
{
    protected final ProcessDescriptorDecoder descriptorDecoder = new ProcessDescriptorDecoder();

    protected int stringIdOffset;

    @Override
    public ProcessGraph wrap(byte[] buffer)
    {
        return (ProcessGraph) super.wrap(buffer);
    }

    @Override
    public ProcessGraph wrap(DirectBuffer src, int offset, int lenght)
    {
        return (ProcessGraph) super.wrap(src, offset, lenght);
    }

    @Override
    protected void init()
    {
        super.init();
        descriptorDecoder.wrap(buffer, dataOffset, ProcessDescriptorDecoder.BLOCK_LENGTH, ProcessDescriptorDecoder.SCHEMA_VERSION);
        stringIdOffset = descriptorDecoder.limit();
    }

    public long id()
    {
        return descriptorDecoder.id();
    }

    public int intialFlowNodeId()
    {
        return descriptorDecoder.intialFlowNodeId();
    }

    public int stringIdBytesLength()
    {
        descriptorDecoder.limit(stringIdOffset);
        return descriptorDecoder.stringIdLength();
    }

    public int getStringId(MutableDirectBuffer dst, int dstOffset, int length)
    {
        descriptorDecoder.limit(stringIdOffset);
        return descriptorDecoder.getStringId(dst, dstOffset, length);
    }

    public int getStringId(byte[] dst, int dstOffset, int length)
    {
        descriptorDecoder.limit(stringIdOffset);
        return descriptorDecoder.getStringId(dst, dstOffset, length);
    }

    public String stringId()
    {
        descriptorDecoder.limit(stringIdOffset);
        return descriptorDecoder.stringId();
    }

}
