package org.camunda.tngp.compactgraph.bpmn;

import static org.camunda.tngp.broker.test.util.BufferAssert.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.broker.test.util.bpmn.TngpModelInstance.wrap;
import static org.camunda.tngp.compactgraph.bpmn.TestUtil.nodeIdByStringId;

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

public class ServiceTaskTest
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
    public void shouldEncodeId()
    {
        // given
        final BpmnModelInstance process = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .serviceTask("serviceTask")
                .endEvent("endEventId")
                .done();

        wrap(process).taskAttributes("serviceTask", "foo", 1);

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(process, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "serviceTask"));

        // then
        assertThat(flowElementVisitor.stringId()).isEqualTo("serviceTask");
    }

    @Test
    public void shouldEncodeTaskTypeAndQueueId()
    {
        // given
        final BpmnModelInstance process = Bpmn.createExecutableProcess("processId")
                .startEvent("startEventId")
                .serviceTask("serviceTask")
                .endEvent("endEventId")
                .done();

        wrap(process).taskAttributes("serviceTask", "foobar", 987);

        // when
        final ProcessGraph processGraph = transformer.transformSingleProcess(process, 10L);

        flowElementVisitor.init(processGraph)
            .moveToNode(nodeIdByStringId(processGraph, "serviceTask"));

        // then
        assertThatBuffer(flowElementVisitor.taskType())
            .hasCapacity(6)
            .hasBytes("foobar".getBytes(StandardCharsets.UTF_8));
        assertThat(flowElementVisitor.taskQueueId()).isEqualTo((short) 987);
    }
}
