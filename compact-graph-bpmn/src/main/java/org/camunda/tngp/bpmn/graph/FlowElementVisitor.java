package org.camunda.tngp.bpmn.graph;

import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.*;

import org.camunda.tngp.compactgraph.NodeVisitor;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorDecoder;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorDecoder.EventBehaviorMappingDecoder;
import org.camunda.tngp.graph.bpmn.FlowElementType;
import org.camunda.tngp.graph.bpmn.GroupSizeEncodingDecoder;

public class FlowElementVisitor extends NodeVisitor
{
    protected final FlowElementDescriptorDecoder flowElementDescriptorDecoder = new FlowElementDescriptorDecoder();

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
        flowElementDescriptorDecoder.wrap(buffer, nodeDataOffset(), FlowElementDescriptorDecoder.BLOCK_LENGTH, FlowElementDescriptorDecoder.SCHEMA_VERSION);

        eventBehaviorMappingOffset = flowElementDescriptorDecoder.limit();

        EventBehaviorMappingDecoder behaviorMappingDecoder = flowElementDescriptorDecoder.eventBehaviorMapping();

        stringIdOffset = eventBehaviorMappingOffset
                + (behaviorMappingDecoder.actingBlockLength() * behaviorMappingDecoder.count())
                + GroupSizeEncodingDecoder.ENCODED_LENGTH;
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

    public BpmnAspect aspectFor(ExecutionEventType event)
    {
        flowElementDescriptorDecoder.limit(eventBehaviorMappingOffset);
        EventBehaviorMappingDecoder behaviorMappingDecoder = flowElementDescriptorDecoder.eventBehaviorMapping();

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

    public String stringId()
    {
        flowElementDescriptorDecoder.limit(stringIdOffset);
        return flowElementDescriptorDecoder.stringId();
    }

    public FlowElementType type()
    {
        return flowElementDescriptorDecoder.type();
    }

}
