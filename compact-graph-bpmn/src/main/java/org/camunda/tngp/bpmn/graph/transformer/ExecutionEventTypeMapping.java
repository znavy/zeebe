package org.camunda.tngp.bpmn.graph.transformer;

import static org.camunda.tngp.graph.bpmn.ExecutionEventType.*;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;

public class ExecutionEventTypeMapping
{
    public static Map<Class<? extends BpmnModelElementInstance>, ExecutionEventType> ON_ENTER_EVENTS = new HashMap<>();
    public static Map<Class<? extends BpmnModelElementInstance>, ExecutionEventType> ON_LEAVE_EVENTS = new HashMap<>();

    static
    {
        ON_ENTER_EVENTS.put(Process.class, PROC_INST_CREATED);
        ON_LEAVE_EVENTS.put(Process.class, PROC_INST_COMPLETED);

        ON_ENTER_EVENTS.put(StartEvent.class, EVT_OCCURRED);
        ON_LEAVE_EVENTS.put(StartEvent.class, NULL_VAL);

        ON_ENTER_EVENTS.put(EndEvent.class, EVT_OCCURRED);
        ON_LEAVE_EVENTS.put(EndEvent.class, NULL_VAL);

        ON_ENTER_EVENTS.put(IntermediateCatchEvent.class, EVT_SUBSCRIBED);
        ON_LEAVE_EVENTS.put(IntermediateCatchEvent.class, EVT_OCCURRED);

        ON_ENTER_EVENTS.put(IntermediateThrowEvent.class, EVT_OCCURRED);
        ON_LEAVE_EVENTS.put(IntermediateThrowEvent.class, NULL_VAL);

        ON_ENTER_EVENTS.put(BoundaryEvent.class, EVT_SUBSCRIBED);
        ON_LEAVE_EVENTS.put(BoundaryEvent.class, EVT_OCCURRED);

        ON_ENTER_EVENTS.put(SequenceFlow.class, SQF_EXECUTED);
        ON_LEAVE_EVENTS.put(SequenceFlow.class, NULL_VAL);

        ON_ENTER_EVENTS.put(ServiceTask.class, ACT_INST_CREATED);
        ON_LEAVE_EVENTS.put(ServiceTask.class, ACT_INST_COMPLETED);

        ON_ENTER_EVENTS.put(UserTask.class, ACT_INST_CREATED);
        ON_LEAVE_EVENTS.put(UserTask.class, ACT_INST_COMPLETED);

        ON_ENTER_EVENTS.put(ExclusiveGateway.class, GW_ACTIVATED);
        ON_ENTER_EVENTS.put(ParallelGateway.class, GW_ACTIVATED);
    }

    public static ExecutionEventType onEnterEvent(Class<? extends ModelElementInstance> modelType)
    {
        return ON_ENTER_EVENTS.get(modelType);
    }

    public static ExecutionEventType onLeaveEvent(Class<? extends ModelElementInstance> modelType)
    {
        return ON_LEAVE_EVENTS.get(modelType);
    }
}
