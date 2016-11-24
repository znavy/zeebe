package org.camunda.tngp.broker.wf.runtime.log.handler.bpmn;

import org.camunda.tngp.bpmn.graph.BpmnEdgeTypes;
import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.broker.log.LogWriters;
import org.camunda.tngp.broker.wf.runtime.log.bpmn.BpmnFlowElementEventReader;
import org.camunda.tngp.broker.wf.runtime.log.bpmn.BpmnFlowElementEventWriter;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.log.idgenerator.IdGenerator;

public class ActivateGatewayHandler implements BpmnFlowElementAspectHandler
{

    protected final BpmnFlowElementEventWriter eventWriter = new BpmnFlowElementEventWriter();
    protected final FlowElementVisitor flowElementVisitor = new FlowElementVisitor();

    @Override
    public int handle(BpmnFlowElementEventReader flowElementEventReader, ProcessGraph process, LogWriters logWriters,
            IdGenerator idGenerator)
    {
        final int sequenceFlowId = flowElementEventReader.flowElementId();
        flowElementVisitor.init(process).moveToNode(sequenceFlowId);

        flowElementVisitor.traverseEdge(BpmnEdgeTypes.SEQUENCE_FLOW_TARGET_NODE);

        eventWriter
            .eventType(ExecutionEventType.GW_ACTIVATED)
            .flowElementId(flowElementVisitor.nodeId())
            .key(idGenerator.nextId())
            .bpmnBranchKey(flowElementEventReader.bpmnBranchKey())
            .workflowInstanceId(flowElementEventReader.wfInstanceId())
            .processId(flowElementEventReader.wfDefinitionId())
            .flowElementIdString(flowElementVisitor.stringIdBuffer(), 0, flowElementVisitor.stringIdBytesLength());

        logWriters.writeToCurrentLog(eventWriter);

        return 0;
    }

    @Override
    public BpmnAspect getHandledBpmnAspect()
    {
        return BpmnAspect.ACTIVE_GATEWAY;
    }

}
