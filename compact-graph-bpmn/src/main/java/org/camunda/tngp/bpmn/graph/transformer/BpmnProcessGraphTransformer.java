package org.camunda.tngp.bpmn.graph.transformer;

import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.EDGE_TYPE_COUNT;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.NODE_INCOMMING_SEQUENCE_FLOWS;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.NODE_OUTGOING_SEQUENCE_FLOWS;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.SEQUENCE_FLOW_SOURCE_NODE;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.SEQUENCE_FLOW_TARGET_NODE;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.bpmn.graph.transformer.aspect.BpmnAspectHandlers;
import org.camunda.tngp.bpmn.graph.transformer.element.BpmnElementTransformers;
import org.camunda.tngp.bpmn.graph.transformer.element.FlowElementDescriptorWriter;
import org.camunda.tngp.compactgraph.GraphEncoder;
import org.camunda.tngp.compactgraph.builder.GraphBuilder;
import org.camunda.tngp.compactgraph.builder.NodeBuilder;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.graph.bpmn.FlowElementDescriptorEncoder;
import org.camunda.tngp.graph.bpmn.MessageHeaderEncoder;
import org.camunda.tngp.graph.bpmn.ProcessDescriptorEncoder;

public class BpmnProcessGraphTransformer
{
    protected static final float UTF8_MAX_CHARS_PER_BYTE = StandardCharsets.UTF_8.newDecoder().maxCharsPerByte();

    protected final GraphBuilder graphBuilder = new GraphBuilder();
    protected final Process process;
    protected final long id;
    protected final FlowElementDescriptorEncoder flowElementDescriptorEncoder = new FlowElementDescriptorEncoder();
    protected final ProcessDescriptorEncoder processDescriptorEncoder = new ProcessDescriptorEncoder();

    protected Map<String, Integer> nodeIdMap = new HashMap<>();

    public BpmnProcessGraphTransformer(Process process, long id)
    {
        this.process = process;
        this.id = id;
        graphBuilder.edgeTypeCount(EDGE_TYPE_COUNT);
    }

    public ProcessGraph transform()
    {
        createProcessNode();
        createFlowElements();
        connectSequenceFlows(process);
        writeProcessData();
        return encodeGraph();
    }

    protected void createProcessNode()
    {
        final NodeBuilder nodeBuilder = graphBuilder
            .newNode()
            .nodeData(encodeFlowElementData(process));

        nodeIdMap.put(process.getId(), nodeBuilder.id());
    }

    protected void writeProcessData()
    {
        final String processId = process.getId();

        final int processDataBufferLength = processDescriptorEncoder.sbeBlockLength() +
                FlowElementDescriptorEncoder.stringIdHeaderLength() +
                (int) Math.ceil(processId.length() / UTF8_MAX_CHARS_PER_BYTE);

        final byte[] dataBuffer = new byte[processDataBufferLength];

        processDescriptorEncoder.wrap(new UnsafeBuffer(dataBuffer), 0)
            .id(id)
            .intialFlowNodeId(findInitialFlowNode(process))
            .stringId(processId);

        graphBuilder.graphData(dataBuffer);
    }

    private int findInitialFlowNode(BpmnModelElementInstance scope)
    {
        final Collection<StartEvent> startEvents = scope.getChildElementsByType(StartEvent.class);

        for (StartEvent startEvent : startEvents)
        {
            if (startEvent.getEventDefinitions().isEmpty())
            {
                return nodeIdMap.get(startEvent.getId());
            }
        }

        throw new RuntimeException("Cannot find none-start event");
    }

    protected ProcessGraph encodeGraph()
    {
        final byte[] encodedGraph = new GraphEncoder(graphBuilder).encode();
        return new ProcessGraph().wrap(encodedGraph);
    }

    protected void connectSequenceFlows(BpmnModelElementInstance scope)
    {
        final Collection<SequenceFlow> sequenceFlows = scope.getChildElementsByType(SequenceFlow.class);

        for (SequenceFlow sequenceFlow : sequenceFlows)
        {
            final int sequenceFlowNodeId = nodeIdMap.get(sequenceFlow.getId());
            final int sourceNodeId = nodeIdMap.get(sequenceFlow.getSource().getId());
            final int targetNodeId = nodeIdMap.get(sequenceFlow.getTarget().getId());

            graphBuilder.node(sequenceFlowNodeId)
                .connect(sourceNodeId, SEQUENCE_FLOW_SOURCE_NODE, NODE_OUTGOING_SEQUENCE_FLOWS);

            graphBuilder.node(sequenceFlowNodeId)
                .connect(targetNodeId, SEQUENCE_FLOW_TARGET_NODE, NODE_INCOMMING_SEQUENCE_FLOWS);
        }

        final Collection<SubProcess> subProcesses = scope.getChildElementsByType(SubProcess.class);
        for (SubProcess subProcess : subProcesses)
        {
            connectSequenceFlows(subProcess);
        }
    }

    protected void createFlowElements()
    {
        final TreeSet<FlowElement> flowElements = new TreeSet<>((o1, o2) -> o1.getId().compareTo(o2.getId()));

        collectFlowElements(process, flowElements);

        for (FlowElement flowElement : flowElements)
        {
            final NodeBuilder nodeBuilder = graphBuilder
                .newNode()
                .nodeData(encodeFlowElementData(flowElement));

            nodeIdMap.put(flowElement.getId(), nodeBuilder.id());
        }
    }

    protected byte[] encodeFlowElementData(final BaseElement element)
    {
        final FlowElementDescriptorWriter writer = new FlowElementDescriptorWriter();

        BpmnElementTransformers.applyTransformers(element, writer);

        final Map<ExecutionEventType, BpmnAspect> aspectMap = BpmnAspectHandlers.getBehavioralAspects(element);
        writer.bpmnAspects(aspectMap);

        final byte[] nodeData = new byte[writer.getLength()];
        final UnsafeBuffer nodeDataBuffer = new UnsafeBuffer(nodeData);

        writer.write(nodeDataBuffer, 0);
        final int lengthWithoutHeader = nodeData.length - MessageHeaderEncoder.ENCODED_LENGTH;
        final byte[] nodeDataWithoutHeader = new byte[lengthWithoutHeader];
        System.arraycopy(nodeData, MessageHeaderEncoder.ENCODED_LENGTH, nodeDataWithoutHeader, 0, lengthWithoutHeader);

        return nodeDataWithoutHeader;
    }

    private static void collectFlowElements(BpmnModelElementInstance scope, TreeSet<FlowElement> flowElements)
    {
        final Collection<FlowElement> childElements = scope.getChildElementsByType(FlowElement.class);
        flowElements.addAll(childElements);

        for (FlowElement flowElement : childElements)
        {
            collectFlowElements(flowElement, flowElements);
        }
    }

}