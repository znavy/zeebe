package org.camunda.tngp.compactgraph.bpmn;

import static org.camunda.tngp.compactgraph.bpmn.BpmnProcessGraphEdgeTypes.*;

import org.camunda.tngp.compactgraph.GraphNavigator;
import org.camunda.tngp.taskqueue.protocol.BpmnGraphNodeType;
import org.camunda.tngp.taskqueue.protocol.FlowElementDataDecoder;

public class FlowNodeVisitor extends GraphNavigator
{
    protected final FlowElementDataDecoder flowElementDataDecoder = new FlowElementDataDecoder();

    @Override
    protected void setOffset(int nodeOffset)
    {
        super.setOffset(nodeOffset);
        flowElementDataDecoder.wrap(buffer, nodeDataOffset(), FlowElementDataDecoder.BLOCK_LENGTH, FlowElementDataDecoder.SCHEMA_VERSION);
    }

    public boolean hasIncomingSequenceFlows()
    {
        return incomingSequenceFlowsCount() > 0;
    }

    public boolean hasOutgoingSequenceFlows()
    {
        return outgoingSequenceFlowsCount() > 0;
    }

    public int incomingSequenceFlowsCount()
    {
        return edgeCount(NODE_INCOMMING_SEQUENCE_FLOWS);
    }

    public int outgoingSequenceFlowsCount()
    {
        return edgeCount(NODE_OUTGOING_SEQUENCE_FLOWS);
    }

    public FlowNodeVisitor traverseSingleOutgoingSequenceFlow()
    {
        traverseEdge(NODE_OUTGOING_SEQUENCE_FLOWS);
        traverseEdge(SEQUENCE_FLOW_TARGET_NODE);

        return this;
    }

    public FlowNodeVisitor traverseSingleIncomingSequenceFlow()
    {
        traverseEdge(NODE_INCOMMING_SEQUENCE_FLOWS);
        traverseEdge(SEQUENCE_FLOW_SOURCE_NODE);

        return this;
    }

    public FlowNodeVisitor traverseOutgoingSequenceFlow(int index)
    {
        traverseEdge(NODE_OUTGOING_SEQUENCE_FLOWS, index);
        traverseEdge(SEQUENCE_FLOW_TARGET_NODE);

        return this;
    }

    public FlowNodeVisitor traverseIncomingSequenceFlow(int index)
    {
        traverseEdge(NODE_INCOMMING_SEQUENCE_FLOWS, index);
        traverseEdge(SEQUENCE_FLOW_SOURCE_NODE);

        return this;
    }

    public String getStringId()
    {
        return flowElementDataDecoder.stringId();
    }

    public BpmnGraphNodeType getType()
    {
        return flowElementDataDecoder.type();
    }
}
