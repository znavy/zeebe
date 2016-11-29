package org.camunda.tngp.compactgraph.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.bpmn.graph.transformer.BpmnModelInstanceTransformer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.agrona.concurrent.UnsafeBuffer;

public class FlowElementCommonPropertiesTest
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
    public void shouldEncodeProperties()
    {
        final String expectedSringId = "startEventId";
        final byte[] expectedStringIdBytes = expectedSringId.getBytes(StandardCharsets.UTF_8);

        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(processGraph.intialFlowNodeId());

        // then
        assertThat(flowElementVisitor.stringId()).isEqualTo(expectedSringId);
        assertThat(flowElementVisitor.stringIdBytesLength()).isEqualTo(expectedStringIdBytes.length);

        final byte[] readBuffer = new byte[expectedStringIdBytes.length];
        flowElementVisitor.getStringId(readBuffer, 0, readBuffer.length);
        assertThat(readBuffer).isEqualTo(expectedStringIdBytes);

        final UnsafeBuffer readBufferView = new UnsafeBuffer(readBuffer);
        readBufferView.setMemory(0, 0, (byte) 0);
        flowElementVisitor.getStringId(readBufferView, 0, readBufferView.capacity());
        assertThat(readBuffer).isEqualTo(expectedStringIdBytes);
    }

    @Test
    public void shouldEncodeSequenceFlows()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .sequenceFlowId("flowId")
                .endEvent()
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(processGraph.intialFlowNodeId());

        // then
        assertThat(flowElementVisitor.hasOutgoingSequenceFlows()).isTrue();
        assertThat(flowElementVisitor.outgoingSequenceFlowsCount()).isEqualTo(1);
        assertThat(flowElementVisitor.hasOutgoingSequenceFlows()).isTrue();
    }

}
