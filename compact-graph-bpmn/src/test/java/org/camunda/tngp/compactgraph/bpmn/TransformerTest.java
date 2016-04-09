package org.camunda.tngp.compactgraph.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.compactgraph.bpmn.transformer.BpmnModelInstanceTransformer;
import org.camunda.tngp.taskqueue.protocol.ProcessDataDecoder;
import org.junit.Before;
import org.junit.Test;

public class TransformerTest
{
    BpmnModelInstanceTransformer transformer;
    FlowNodeVisitor flowNodeVisitor;

    @Before
    public void setup()
    {
        transformer = new BpmnModelInstanceTransformer();
        flowNodeVisitor = new FlowNodeVisitor();
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

        final BpmnProcessGraph processGraph = transformer.transpformSingleProcess(theProcess);

        final ProcessDataDecoder processData = processGraph.getProcessData();
        final int initialFlowNode = processData.intialFlowNode();
        assertThat(initialFlowNode != ProcessDataDecoder.intialFlowNodeNullValue());
        assertThat(processData.stringId()).isEqualTo(processId);

        flowNodeVisitor.init(processGraph).moveToNode(initialFlowNode);

        assertThat(flowNodeVisitor.getStringId()).isEqualTo(startEventId);
        assertThat(flowNodeVisitor.hasOutgoingSequenceFlows()).isTrue();
        assertThat(flowNodeVisitor.hasIncomingSequenceFlows()).isFalse();

        flowNodeVisitor.traverseSingleOutgoingSequenceFlow();

        assertThat(flowNodeVisitor.getStringId()).isEqualTo(endEventId);
        assertThat(flowNodeVisitor.hasOutgoingSequenceFlows()).isFalse();
        assertThat(flowNodeVisitor.hasIncomingSequenceFlows()).isTrue();

        flowNodeVisitor.traverseSingleIncomingSequenceFlow();
        assertThat(flowNodeVisitor.getStringId()).isEqualTo(startEventId);
    }

}
