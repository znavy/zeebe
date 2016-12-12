package org.camunda.tngp.bpmn.graph;

import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.NODE_INCOMMING_SEQUENCE_FLOWS;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.NODE_OUTGOING_SEQUENCE_FLOWS;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.SEQUENCE_FLOW_SOURCE_NODE;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.SEQUENCE_FLOW_TARGET_NODE;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.compactgraph.NodeVisitor;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ConditionOperator;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorDecoder;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorDecoder.EventBehaviorMappingDecoder;
import org.camunda.tngp.graph.bpmn.FlowElementType;
import org.camunda.tngp.graph.bpmn.GroupSizeEncodingDecoder;

public class FlowElementVisitor extends NodeVisitor
{
    protected final FlowElementDescriptorDecoder descriptorDecoder = new FlowElementDescriptorDecoder();

    protected int stringIdOffset;
    protected int eventBehaviorMappingOffset;

    protected final UnsafeBuffer stringIdBuffer = new UnsafeBuffer(0, 0);
    protected final UnsafeBuffer taskTypeBuffer = new UnsafeBuffer(0, 0);

    protected final MsgPackPropertyReader conditionArg1Reader = new MsgPackPropertyReader();
    protected final MsgPackPropertyReader conditionArg2Reader = new MsgPackPropertyReader();

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

        descriptorDecoder.limit(stringIdOffset);
        final int stringIdLength = descriptorDecoder.stringIdLength();
        stringIdBuffer.wrap(buffer, stringIdOffset + FlowElementDescriptorDecoder.stringIdHeaderLength(), stringIdLength);

        final int taskTypeOffset = stringIdOffset +
                FlowElementDescriptorDecoder.stringIdHeaderLength() +
                stringIdLength;

        descriptorDecoder.limit(taskTypeOffset);

        final int taskTypeLength = descriptorDecoder.taskTypeLength();
        taskTypeBuffer.wrap(buffer, taskTypeOffset + FlowElementDescriptorDecoder.taskTypeHeaderLength(), taskTypeLength);


        final int conditionArg1Offset = taskTypeOffset +
                FlowElementDescriptorDecoder.taskTypeHeaderLength() +
                taskTypeLength;

        descriptorDecoder.limit(conditionArg1Offset);

        final int conditionArg1Length = descriptorDecoder.conditionArg1Length();
        conditionArg1Reader.wrap(descriptorDecoder.conditionArg1Type(),
                buffer,
                conditionArg1Offset + FlowElementDescriptorDecoder.conditionArg1HeaderLength(),
                conditionArg1Length);

        final int conditionArg2Offset = conditionArg1Offset +
                FlowElementDescriptorDecoder.conditionArg1HeaderLength() +
                conditionArg1Length;

        descriptorDecoder.limit(conditionArg2Offset);

        final int conditionArg2Length = descriptorDecoder.conditionArg2Length();

        conditionArg2Reader.wrap(descriptorDecoder.conditionArg2Type(),
                buffer,
                conditionArg2Offset + FlowElementDescriptorDecoder.conditionArg2HeaderLength(),
                conditionArg2Length);
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

    @Override
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

    public DirectBuffer taskType()
    {
        return taskTypeBuffer;
    }

    public DirectBuffer stringIdBuffer()
    {
        return stringIdBuffer;
    }

    public short taskQueueId()
    {
        return descriptorDecoder.taskQueueId();
    }

    public MsgPackPropertyReader conditionArg1()
    {
        return conditionArg1Reader;
    }
    public MsgPackPropertyReader conditionArg2()
    {
        return conditionArg2Reader;
    }

    public ConditionOperator conditionOperator()
    {
        return descriptorDecoder.conditionOperator();
    }

    public boolean isDefaultFlow()
    {
        return descriptorDecoder.isDefault() != 0;
    }


}
