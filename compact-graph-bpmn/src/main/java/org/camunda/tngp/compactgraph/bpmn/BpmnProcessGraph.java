package org.camunda.tngp.compactgraph.bpmn;

import org.camunda.tngp.compactgraph.Graph;
import org.camunda.tngp.taskqueue.protocol.ProcessDataDecoder;

import uk.co.real_logic.agrona.DirectBuffer;

public class BpmnProcessGraph extends Graph
{
    protected final ProcessDataDecoder processDataDecoder = new ProcessDataDecoder();

    @Override
    public BpmnProcessGraph wrap(byte[] buffer)
    {
        return (BpmnProcessGraph) super.wrap(buffer);
    }

    @Override
    public BpmnProcessGraph wrap(DirectBuffer src, int offset, int lenght)
    {
        return (BpmnProcessGraph) super.wrap(src, offset, lenght);
    }

    @Override
    protected void init()
    {
        super.init();

        processDataDecoder.wrap(buffer, dataOffset, ProcessDataDecoder.BLOCK_LENGTH, ProcessDataDecoder.SCHEMA_VERSION);
    }

    public ProcessDataDecoder getProcessData()
    {
        return processDataDecoder;
    }

}
