package org.camunda.tngp.compactgraph.bpmn;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.compactgraph.bpmn.TestUtil.nodeIdByStringId;
import static org.camunda.tngp.graph.bpmn.FlowElementType.PROCESS;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.bpmn.graph.transformer.BpmnModelInstanceTransformer;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.agrona.concurrent.UnsafeBuffer;

public class ProcessTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    BpmnModelInstanceTransformer transformer;
    FlowElementVisitor flowNodeVisitor;

    @Before
    public void setup()
    {
        transformer = new BpmnModelInstanceTransformer();
        flowNodeVisitor = new FlowElementVisitor();
    }

    @Test
    public void shouldEncodeProvidedId()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent()
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        // then
        assertThat(processGraph.id()).isEqualTo(10L);
    }

    @Test
    public void shoulEncodeGraphProperties()
    {
        final String expectedSringId = "processId";
        final byte[] expectedStringIdBytes = expectedSringId.getBytes(UTF_8);

        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent()
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        // then
        assertThat(processGraph.stringId()).isEqualTo(expectedSringId);
        assertThat(processGraph.stringIdBytesLength()).isEqualTo(expectedStringIdBytes.length);

        final byte[] readBuffer = new byte[expectedStringIdBytes.length];
        processGraph.getStringId(readBuffer, 0, readBuffer.length);
        assertThat(readBuffer).isEqualTo(expectedStringIdBytes);

        final UnsafeBuffer readBufferView = new UnsafeBuffer(readBuffer);
        readBufferView.setMemory(0, 0, (byte) 0);
        processGraph.getStringId(readBufferView, 0, readBufferView.capacity());
        assertThat(readBuffer).isEqualTo(expectedStringIdBytes);
    }

    @Test
    public void shoulEncodeNodeProperties()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent()
                .done();

        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        // when
        flowNodeVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "processId"));

        // then
        assertThat(flowNodeVisitor.type()).isEqualTo(PROCESS);
    }

    @Test
    public void shouldEncodeInitialNodeId()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent()
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        // then
        assertThat(processGraph.intialFlowNodeId()).isGreaterThan(0);
    }

    @Test
    public void shouldEncodeAspects()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .done();

        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowNodeVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "processId"));

        // then
        assertThat(flowNodeVisitor.aspectFor(ExecutionEventType.PROC_INST_CREATED)).isEqualTo(BpmnAspect.TAKE_OUTGOING_FLOWS);
    }

}
