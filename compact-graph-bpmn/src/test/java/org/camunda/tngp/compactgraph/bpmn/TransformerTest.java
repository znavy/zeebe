package org.camunda.tngp.compactgraph.bpmn;

import static org.assertj.core.api.Assertions.*;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.bpmn.graph.transformer.BpmnModelInstanceTransformer;
import org.camunda.tngp.graph.bpmn.ProcessDescriptorDecoder;
import org.junit.Before;
import org.junit.Test;

public class TransformerTest
{
    BpmnModelInstanceTransformer transformer;
    FlowElementVisitor flowNodeVisitor;

    @Before
    public void setup()
    {
        transformer = new BpmnModelInstanceTransformer();
        flowNodeVisitor = new FlowElementVisitor();
    }

    @Test
    public void shouldTransformProcess()
    {
        final String processId = "theProcessId";
        final String startEventId = "theStart";
        final String endEventId = "theEnd";

        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess(processId)
                .startEvent(startEventId)
                .endEvent(endEventId)
                .done();

        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        assertThat(processGraph.id()).isEqualTo(10L);

        final int initialFlowNode = processGraph.intialFlowNodeId();
        assertThat(initialFlowNode != ProcessDescriptorDecoder.intialFlowNodeIdNullValue());
        assertThat(processGraph.stringId()).isEqualTo(processId);

        flowNodeVisitor.init(processGraph).moveToNode(initialFlowNode);

        assertThat(flowNodeVisitor.stringId()).isEqualTo(startEventId);
        assertThat(flowNodeVisitor.hasOutgoingSequenceFlows()).isTrue();
        assertThat(flowNodeVisitor.hasIncomingSequenceFlows()).isFalse();

        flowNodeVisitor.traverseSingleOutgoingSequenceFlow();

        assertThat(flowNodeVisitor.stringId()).isEqualTo(endEventId);
        assertThat(flowNodeVisitor.hasOutgoingSequenceFlows()).isFalse();
        assertThat(flowNodeVisitor.hasIncomingSequenceFlows()).isTrue();

        flowNodeVisitor.traverseSingleIncomingSequenceFlow();

        assertThat(flowNodeVisitor.stringId()).isEqualTo(startEventId);
    }

}
