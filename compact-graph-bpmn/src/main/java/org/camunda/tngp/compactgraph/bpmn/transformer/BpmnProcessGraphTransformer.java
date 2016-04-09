package org.camunda.tngp.compactgraph.bpmn.transformer;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.camunda.tngp.compactgraph.GraphEncoder;
import org.camunda.tngp.compactgraph.bpmn.BpmnProcessGraph;
import org.camunda.tngp.compactgraph.builder.GraphBuilder;
import org.camunda.tngp.compactgraph.builder.NodeBuilder;
import org.camunda.tngp.taskqueue.protocol.FlowElementDataEncoder;
import org.camunda.tngp.taskqueue.protocol.ProcessDataEncoder;

import static org.camunda.tngp.compactgraph.bpmn.BpmnProcessGraphEdgeTypes.*;

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class BpmnProcessGraphTransformer
{
    protected final static float UTF8_MAX_CHARS_PER_BYTE = StandardCharsets.UTF_8.newDecoder().maxCharsPerByte();

    protected final GraphBuilder graphBuilder = new GraphBuilder();
    protected final Process process;
    protected final FlowElementDataEncoder flowElementDataEncoder = new FlowElementDataEncoder();
    protected final ProcessDataEncoder processDataEncoder = new ProcessDataEncoder();

    protected Map<String, Integer> nodeIdMap = new HashMap<>();

    public BpmnProcessGraphTransformer(Process process)
    {
        this.process = process;
        graphBuilder.edgeTypeCount(EDGE_TYPE_COUNT);
    }

    public BpmnProcessGraph transform()
    {
        createFlowElements();
        connectSequenceFlows(process);
        writeProcessData();
        return encodeGraph();
    }

    protected void writeProcessData()
    {
        final String processId = process.getId();

        final int processDataBufferLength = processDataEncoder.sbeBlockLength() +
                ProcessDataEncoder.stringIdHeaderLength() +
                (int) Math.ceil(processId.length() / UTF8_MAX_CHARS_PER_BYTE);

        final byte[] dataBuffer = new byte[processDataBufferLength];

        processDataEncoder.wrap(new UnsafeBuffer(dataBuffer), 0)
            .intialFlowNode(findInitialFlowNode(process))
            .stringId(processId);

        graphBuilder.graphData(dataBuffer);
    }

    private int findInitialFlowNode(BpmnModelElementInstance scope)
    {
        final Collection<StartEvent> startEvents = scope.getChildElementsByType(StartEvent.class);

        for (StartEvent startEvent : startEvents)
        {
            if(startEvent.getEventDefinitions().isEmpty())
            {
                return nodeIdMap.get(startEvent.getId());
            }
        }

        throw new RuntimeException("Cannot find none-start event");
    }

    protected BpmnProcessGraph encodeGraph()
    {
        final byte[] encodedGraph = new GraphEncoder(graphBuilder).encode();
        return new BpmnProcessGraph().wrap(encodedGraph);
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

    protected byte[] encodeFlowElementData(FlowElement flowElement)
    {
        final ModelElementType elementType = flowElement.getElementType();
        final String id = flowElement.getId();

        final int maxNodeDataLength = flowElementDataEncoder.sbeBlockLength() + FlowElementDataEncoder.stringIdHeaderLength() + (int) Math.ceil(id.length() / UTF8_MAX_CHARS_PER_BYTE);
        final byte[] nodeDataBuffer = new byte[maxNodeDataLength];

        flowElementDataEncoder.wrap(new UnsafeBuffer(nodeDataBuffer), 0)
            .type(BpmnGraphNodeTypeMap.graphNodeTypeForModelType(elementType.getInstanceType()))
            .stringId(id);

        return nodeDataBuffer;
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
