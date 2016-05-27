package org.camunda.tngp.bpmn.graph;

import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.*;

import org.camunda.tngp.compactgraph.NodeVisitor;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorDecoder;
import org.camunda.tngp.graph.bpmn.FlowElementType;

public class FlowElementVisitor extends NodeVisitor
{
    protected final FlowElementDescriptorDecoder flowElementDescriptorDecoder = new FlowElementDescriptorDecoder();

    public FlowElementVisitor init(ProcessGraph graph)
    {
        super.init(graph);
        return this;
    }

    @Override
    protected void setOffset(int nodeOffset)
    {
        super.setOffset(nodeOffset);
        flowElementDescriptorDecoder.wrap(buffer, nodeDataOffset(), FlowElementDescriptorDecoder.BLOCK_LENGTH, FlowElementDescriptorDecoder.SCHEMA_VERSION);
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

    public FlowElementVisitor traverseSingleOutgoingSequenceFlow()
    {
        traverseEdge(NODE_OUTGOING_SEQUENCE_FLOWS);
        traverseEdge(SEQUENCE_FLOW_TARGET_NODE);

        return this;
    }

    public FlowElementVisitor traverseSingleIncomingSequenceFlow()
    {
        traverseEdge(NODE_INCOMMING_SEQUENCE_FLOWS);
        traverseEdge(SEQUENCE_FLOW_SOURCE_NODE);

        return this;
    }

    public FlowElementVisitor traverseOutgoingSequenceFlow(int index)
    {
        traverseEdge(NODE_OUTGOING_SEQUENCE_FLOWS, index);
        traverseEdge(SEQUENCE_FLOW_TARGET_NODE);

        return this;
    }

    public FlowElementVisitor traverseIncomingSequenceFlow(int index)
    {
        traverseEdge(NODE_INCOMMING_SEQUENCE_FLOWS, index);
        traverseEdge(SEQUENCE_FLOW_SOURCE_NODE);

        return this;
    }

    public String stringId()
    {
        return flowElementDescriptorDecoder.stringId();
    }

    public FlowElementType type()
    {
        return flowElementDescriptorDecoder.type();
    }

    public ExecutionEventType onEnterEvent()
    {
        return flowElementDescriptorDecoder.onEnterEvent();
    }

    public ExecutionEventType onLeaveEvent()
    {
        return flowElementDescriptorDecoder.onLeaveEvent();
    }

}
