package org.camunda.tngp.compactgraph.bpmn;

import static org.assertj.core.api.Assertions.*;
import static org.camunda.tngp.bpmn.graph.BpmnEdgeTypes.*;
import static org.camunda.tngp.compactgraph.bpmn.TestUtil.*;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.bpmn.graph.transformer.BpmnModelInstanceTransformer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SequenceFlowTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    BpmnModelInstanceTransformer transformer;
    FlowElementVisitor flowElementVisitor;

    @Before
    public void setup()
    {
        transformer = new BpmnModelInstanceTransformer();
        flowElementVisitor = new FlowElementVisitor();
    }

    @Test
    public void shoulEncodeSingleOutgoingSequenceFlowEdge()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .endEvent("endEventId")
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "startEventId"));

        // then
        assertThat(flowElementVisitor.edgeCount(NODE_OUTGOING_SEQUENCE_FLOWS)).isEqualTo(1);
    }

    @Test
    public void shoulEncodeMultipleOutgoingSequenceFlowEdges()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .endEvent("endEvent1")
                .moveToNode("startEventId")
                .endEvent("endEvent2")
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "startEventId"));

        // then
        assertThat(flowElementVisitor.edgeCount(NODE_OUTGOING_SEQUENCE_FLOWS)).isEqualTo(2);
    }

    @Test
    public void shoulEncodeSingleIncomingSequenceFlowEdge()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .endEvent("endEventId")
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "endEventId"));

        // then
        assertThat(flowElementVisitor.edgeCount(NODE_INCOMMING_SEQUENCE_FLOWS)).isEqualTo(1);
    }

    @Test
    public void shoulEncodeMultipleIncomingSequenceFlowEdges()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .endEvent("endEventId")
                .moveToNode("startEventId")
                .connectTo("endEventId")
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "endEventId"));

        // then
        assertThat(flowElementVisitor.edgeCount(NODE_INCOMMING_SEQUENCE_FLOWS)).isEqualTo(2);
    }

    @Test
    public void shoulEncodeSequenceFlowSourceAndTarget()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .sequenceFlowId("sequenceFlowId")
                .endEvent("endEventId")
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "sequenceFlowId"));

        // then
        assertThat(flowElementVisitor.edgeCount(SEQUENCE_FLOW_SOURCE_NODE)).isEqualTo(1);
        assertThat(flowElementVisitor.edgeCount(SEQUENCE_FLOW_TARGET_NODE)).isEqualTo(1);
    }

    @Test
    public void shouldConnectCorrectly()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .sequenceFlowId("sequenceFlowId")
                .endEvent("endEventId")
                .done();

        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "startEventId"));

        flowElementVisitor.traverseEdge(NODE_OUTGOING_SEQUENCE_FLOWS);
        assertThat(flowElementVisitor.stringId()).isEqualTo("sequenceFlowId");

        flowElementVisitor.traverseEdge(SEQUENCE_FLOW_TARGET_NODE);
        assertThat(flowElementVisitor.stringId()).isEqualTo("endEventId");

        flowElementVisitor.traverseEdge(NODE_INCOMMING_SEQUENCE_FLOWS);
        assertThat(flowElementVisitor.stringId()).isEqualTo("sequenceFlowId");

        flowElementVisitor.traverseEdge(SEQUENCE_FLOW_SOURCE_NODE);
        assertThat(flowElementVisitor.stringId()).isEqualTo("startEventId");
    }

}
