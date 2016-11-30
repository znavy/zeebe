package org.camunda.tngp.compactgraph.bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.compactgraph.bpmn.TestUtil.nodeIdByStringId;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.bpmn.graph.transformer.BpmnModelInstanceTransformer;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.graph.bpmn.FlowElementType;
import org.junit.Before;
import org.junit.Test;

public class EndEventTest
{
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
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEvent")
                .endEvent("endEvent")
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        // then
        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "endEvent"));

        assertThat(flowElementVisitor.type()).isEqualTo(FlowElementType.END_EVENT);
    }

    @Test
    public void shouldEncodeAspects()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEvent")
                .endEvent("endEvent")
                .done();

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        // then
        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "endEvent"));

        assertThat(flowElementVisitor.aspectFor(ExecutionEventType.EVT_OCCURRED)).isEqualTo(BpmnAspect.END_PROCESS);
    }

}
