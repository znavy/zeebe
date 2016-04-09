package org.camunda.tngp.compactgraph.bpmn.transformer;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.tngp.taskqueue.protocol.BpmnGraphNodeType;

import static org.camunda.tngp.taskqueue.protocol.BpmnGraphNodeType.*;

public class BpmnGraphNodeTypeMap
{
    public static Map<Class<? extends BpmnModelElementInstance>, BpmnGraphNodeType> modelTypeToGraphTypeMap = new HashMap<>();

    static
    {
        modelTypeToGraphTypeMap.put(StartEvent.class, START_EVENT);
        modelTypeToGraphTypeMap.put(EndEvent.class, END_EVENT);
        modelTypeToGraphTypeMap.put(IntermediateCatchEvent.class, INTERMEDIATE_CATCH_EVENT);
        modelTypeToGraphTypeMap.put(IntermediateThrowEvent.class, INTERMEDIATE_THROW_EVENT);
        modelTypeToGraphTypeMap.put(BoundaryEvent.class, BOUNDARY_EVENT);

        modelTypeToGraphTypeMap.put(SequenceFlow.class, SEQUENCE_FLOW);

        modelTypeToGraphTypeMap.put(ServiceTask.class, SERVICE_TASK);
        modelTypeToGraphTypeMap.put(UserTask.class, USER_TASK);

        modelTypeToGraphTypeMap.put(ExclusiveGateway.class, EXCLUSIVE_GATEWAY);
        modelTypeToGraphTypeMap.put(ParallelGateway.class, BpmnGraphNodeType.PARALLEL_GATEWAY);
    }

    public static BpmnGraphNodeType graphNodeTypeForModelType(Class<? extends ModelElementInstance> modelType)
    {
        return modelTypeToGraphTypeMap.get(modelType);
    }

}
