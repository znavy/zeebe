package org.camunda.tngp.compactgraph.bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.compactgraph.bpmn.TestUtil.nodeIdByStringId;
import static org.camunda.tngp.graph.bpmn.BpmnAspect.START_PROCESS;
import static org.camunda.tngp.graph.bpmn.ExecutionEventType.EVT_OCCURRED;
import static org.camunda.tngp.graph.bpmn.FlowElementType.START_EVENT;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.bpmn.graph.transformer.BpmnModelInstanceTransformer;
import org.junit.Before;
import org.junit.Test;

public class StartEventTest
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
                .startEvent("startEventId")
                .done();

        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "startEventId"));

        // then
        assertThat(flowElementVisitor.type()).isEqualTo(START_EVENT);
    }

    @Test
    public void shouldEncodeAspects()
    {
        // given
        final BpmnModelInstance theProcess = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .done();

        final ProcessGraph processGraph = transformer.transformSingleProcess(theProcess, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "startEventId"));

        // then
        assertThat(flowElementVisitor.aspectFor(EVT_OCCURRED)).isEqualTo(START_PROCESS);
    }

}
