package org.camunda.tngp.bpmn.graph;

import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.NODE_INCOMMING_SEQUENCE_FLOWS;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.NODE_OUTGOING_SEQUENCE_FLOWS;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.SEQUENCE_FLOW_SOURCE_NODE;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.SEQUENCE_FLOW_TARGET_NODE;

import org.camunda.tngp.compactgraph.NodeVisitor;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorDecoder;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorDecoder.EventBehaviorMappingDecoder;
import org.camunda.tngp.graph.bpmn.FlowElementType;
import org.camunda.tngp.graph.bpmn.GroupSizeEncodingDecoder;

import uk.co.real_logic.agrona.MutableDirectBuffer;

public class FlowElementVisitor extends NodeVisitor
{
    protected final FlowElementDescriptorDecoder descriptorDecoder = new FlowElementDescriptorDecoder();

    protected int stringIdOffset;
    protected int eventBehaviorMappingOffset;

    public FlowElementVisitor init(ProcessGraph graph)
    {
        super.init(graph);
        return this;
    }

    @Override
    protected void setOffset(int nodeOffset)
    {
        super.setOffset(nodeOffset);
        descriptorDecoder.wrap(buffer, nodeDataOffset(), FlowElementDescriptorDecoder.BLOCK_LENGTH, FlowElementDescriptorDecoder.SCHEMA_VERSION);

        eventBehaviorMappingOffset = descriptorDecoder.limit();

        final EventBehaviorMappingDecoder behaviorMappingDecoder = descriptorDecoder.eventBehaviorMapping();

        stringIdOffset = eventBehaviorMappingOffset +
                (behaviorMappingDecoder.actingBlockLength() * behaviorMappingDecoder.count()) +
                GroupSizeEncodingDecoder.ENCODED_LENGTH;
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

    public FlowElementVisitor moveToNode(int nodeId)
    {
        return (FlowElementVisitor) super.moveToNode(nodeId);
    }

    public BpmnAspect aspectFor(ExecutionEventType event)
    {
        descriptorDecoder.limit(eventBehaviorMappingOffset);
        final EventBehaviorMappingDecoder behaviorMappingDecoder = descriptorDecoder.eventBehaviorMapping();

        while (behaviorMappingDecoder.hasNext())
        {
            if (behaviorMappingDecoder.next().event() == event)
            {
                return behaviorMappingDecoder.behavioralAspect();
            }
        }

        // TODO
        throw new RuntimeException("Event not understood");
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

    public FlowElementType type()
    {
        return descriptorDecoder.type();
    }

}
