package org.camunda.tngp.bpmn.graph.transformer.element;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ConditionOperator;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorEncoder;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorEncoder.EventBehaviorMappingEncoder;
import org.camunda.tngp.graph.bpmn.FlowElementType;
import org.camunda.tngp.graph.bpmn.MessageHeaderEncoder;
import org.camunda.tngp.util.buffer.BufferWriter;

public class FlowElementDescriptorWriter implements BufferWriter
{

    protected FlowElementType flowElementType;
    protected short taskQueueId;
    protected ConditionOperator conditionOperator;

    protected MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    protected FlowElementDescriptorEncoder bodyEncoder = new FlowElementDescriptorEncoder();

    protected UnsafeBuffer stringIdBuffer = new UnsafeBuffer(0, 0);
    protected UnsafeBuffer taskTypeBuffer = new UnsafeBuffer(0, 0);

    protected JsonPropertyWriter arg1Writer = new JsonPropertyWriter();
    protected JsonPropertyWriter arg2Writer = new JsonPropertyWriter();

    protected Map<ExecutionEventType, BpmnAspect> bpmnAspects;

    public FlowElementDescriptorWriter()
    {
        reset();
    }

    protected void reset()
    {
        flowElementType = FlowElementType.NULL_VAL;
        taskQueueId = FlowElementDescriptorEncoder.taskQueueIdNullValue();
        stringIdBuffer.wrap(0, 0);
        taskTypeBuffer.wrap(0, 0);
        bpmnAspects = null;
        conditionOperator = ConditionOperator.NULL_VAL;
        arg1Writer.reset();
        arg2Writer.reset();
    }

    @Override
    public int getLength()
    {
        return MessageHeaderEncoder.ENCODED_LENGTH +
                FlowElementDescriptorEncoder.BLOCK_LENGTH +
                EventBehaviorMappingEncoder.sbeHeaderSize() +
                (EventBehaviorMappingEncoder.sbeBlockLength() * bpmnAspects.size()) +
                FlowElementDescriptorEncoder.stringIdHeaderLength() +
                stringIdBuffer.capacity() +
                FlowElementDescriptorEncoder.taskTypeHeaderLength() +
                taskTypeBuffer.capacity() +
                FlowElementDescriptorEncoder.conditionArg1HeaderLength() +
                arg1Writer.getPropertyBuffer().capacity() +
                FlowElementDescriptorEncoder.conditionArg2HeaderLength() +
                arg2Writer.getPropertyBuffer().capacity();
    }

    public FlowElementDescriptorWriter flowElementType(FlowElementType flowElementType)
    {
        this.flowElementType = flowElementType;
        return this;
    }

    public FlowElementDescriptorWriter taskQueueId(short taskQueueId)
    {
        this.taskQueueId = taskQueueId;
        return this;
    }

    public FlowElementDescriptorWriter id(String id)
    {
        wrapString(id, stringIdBuffer);
        return this;
    }

    public FlowElementDescriptorWriter bpmnAspects(Map<ExecutionEventType, BpmnAspect> executionAspects)
    {
        this.bpmnAspects = executionAspects;
        return this;
    }

    public FlowElementDescriptorWriter taskType(String taskType)
    {
        wrapString(taskType, taskTypeBuffer);
        return this;
    }

    public JsonPropertyWriter conditionArg1()
    {
        return arg1Writer;
    }

    public JsonPropertyWriter conditionArg2()
    {
        return arg2Writer;
    }

    public FlowElementDescriptorWriter conditionOperator(ConditionOperator conditionOperator)
    {
        this.conditionOperator = conditionOperator;
        return this;
    }

    protected void wrapString(String argument, UnsafeBuffer buffer)
    {
        final byte[] bytes = argument.getBytes(StandardCharsets.UTF_8);
        buffer.wrap(bytes);

    }

    @Override
    public void write(MutableDirectBuffer buffer, int offset)
    {
        headerEncoder.wrap(buffer, offset)
            .schemaId(FlowElementDescriptorEncoder.SCHEMA_ID)
            .templateId(FlowElementDescriptorEncoder.TEMPLATE_ID)
            .blockLength(FlowElementDescriptorEncoder.BLOCK_LENGTH)
            .version(FlowElementDescriptorEncoder.SCHEMA_VERSION);

        offset += MessageHeaderEncoder.ENCODED_LENGTH;

        bodyEncoder.wrap(buffer, offset)
            .type(flowElementType)
            .taskQueueId(taskQueueId)
            .conditionArg1Type(arg1Writer.jsonType())
            .conditionArg2Type(arg2Writer.jsonType())
            .conditionOperator(conditionOperator);

        final EventBehaviorMappingEncoder eventBehaviorMappingEncoder =
                bodyEncoder.eventBehaviorMappingCount(bpmnAspects.size());

        for (Map.Entry<ExecutionEventType, BpmnAspect> aspect : bpmnAspects.entrySet())
        {
            eventBehaviorMappingEncoder
                .next()
                .event(aspect.getKey())
                .behavioralAspect(aspect.getValue());
        }

        bodyEncoder.putStringId(stringIdBuffer, 0, stringIdBuffer.capacity());
        bodyEncoder.putTaskType(taskTypeBuffer, 0, taskTypeBuffer.capacity());

        final DirectBuffer conditionArg1Buffer = arg1Writer.getPropertyBuffer();
        final DirectBuffer conditionArg2Buffer = arg2Writer.getPropertyBuffer();
        bodyEncoder.putConditionArg1(conditionArg1Buffer, 0, conditionArg1Buffer.capacity());
        bodyEncoder.putConditionArg2(conditionArg2Buffer, 0, conditionArg2Buffer.capacity());
    }
}
