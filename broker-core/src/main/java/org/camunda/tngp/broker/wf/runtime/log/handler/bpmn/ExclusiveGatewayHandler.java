package org.camunda.tngp.broker.wf.runtime.log.handler.bpmn;

import org.agrona.DirectBuffer;
import org.camunda.tngp.bpmn.graph.BpmnEdgeTypes;
import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.JsonPropertyReader;
import org.camunda.tngp.bpmn.graph.JsonScalarReader;
import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.broker.log.LogWriters;
import org.camunda.tngp.broker.services.JsonConfiguration;
import org.camunda.tngp.broker.wf.runtime.data.JsonDocument;
import org.camunda.tngp.broker.wf.runtime.data.JsonPathResult;
import org.camunda.tngp.broker.wf.runtime.log.bpmn.BpmnBranchEventReader;
import org.camunda.tngp.broker.wf.runtime.log.bpmn.BpmnFlowElementEventReader;
import org.camunda.tngp.broker.wf.runtime.log.bpmn.BpmnFlowElementEventWriter;
import org.camunda.tngp.graph.bpmn.BpmnAspect;
import org.camunda.tngp.graph.bpmn.ConditionOperator;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.graph.bpmn.JsonType;
import org.camunda.tngp.hashindex.Long2LongHashIndex;
import org.camunda.tngp.log.LogReader;
import org.camunda.tngp.log.idgenerator.IdGenerator;
import org.camunda.tngp.util.buffer.BufferUtil;

public class ExclusiveGatewayHandler implements BpmnFlowElementAspectHandler
{

    public static final int NO_FLOW_ID = -1; // note: this assumes flow element IDs are positive!

    protected static final BooleanBiFunction<JsonScalarReader> EQUAL_OPERATOR = new EqualOperator();
    protected static final BooleanBiFunction<JsonScalarReader> GREATER_THAN_OPERATOR = new GreaterThanOperator();
    protected static final BooleanBiFunction<JsonScalarReader> LOWER_THAN_OPERATOR = new LowerThanOperator();

    protected BpmnFlowElementEventWriter eventWriter = new BpmnFlowElementEventWriter();
    protected final FlowElementVisitor flowElementVisitor = new FlowElementVisitor();
    protected BpmnBranchEventReader bpmnBranchEventReader = new BpmnBranchEventReader();

    protected final Long2LongHashIndex eventIndex;
    protected final LogReader logReader;

    protected final JsonDocument jsonDocument;

    public ExclusiveGatewayHandler(LogReader logReader, Long2LongHashIndex eventIndex, JsonConfiguration jsonConfiguration)
    {
        this.eventIndex = eventIndex;
        this.logReader = logReader;
        this.jsonDocument = jsonConfiguration.buildJsonDocument(2);
    }

    @Override
    public BpmnAspect getHandledBpmnAspect()
    {
        return BpmnAspect.EXCLUSIVE_SPLIT;
    }

    @Override
    public int handle(BpmnFlowElementEventReader gatewayEvent, ProcessGraph process, LogWriters logWriters,
            IdGenerator idGenerator)
    {
        flowElementVisitor.init(process);

        final int flowToTake = determineActivatedFlow(gatewayEvent);

        if (flowToTake == NO_FLOW_ID)
        {
            System.err.println("Could not take any of the outgoing sequence flows. Workflow instance " + gatewayEvent.wfInstanceId() + " is stuck and won't continue execution");
        }
        else
        {
            flowElementVisitor.moveToNode(flowToTake);
            takeSequenceFlow(gatewayEvent, flowElementVisitor, logWriters);
        }

        return 0;
    }

    protected int determineActivatedFlow(BpmnFlowElementEventReader gatewayEvent)
    {
        final int gatewayId = gatewayEvent.flowElementId();
        flowElementVisitor.moveToNode(gatewayId);

        final int outgoingSequenceFlowsCount = flowElementVisitor.outgoingSequenceFlowsCount();
        initJsonDocument(gatewayEvent.bpmnBranchKey());

        int sequenceFlowIndex = 0;
        int defaultFlowId = NO_FLOW_ID;
        int flowToTake = NO_FLOW_ID;

        while (sequenceFlowIndex < outgoingSequenceFlowsCount && flowToTake == NO_FLOW_ID)
        {
            flowElementVisitor.moveToNode(gatewayId);
            flowElementVisitor.traverseEdge(BpmnEdgeTypes.NODE_OUTGOING_SEQUENCE_FLOWS, sequenceFlowIndex);

            if (flowElementVisitor.isDefaultFlow())
            {
                defaultFlowId = flowElementVisitor.nodeId();
            }
            else
            {
                final Boolean conditionResult = evaluateCondition(
                        jsonDocument,
                        flowElementVisitor.conditionArg1(),
                        flowElementVisitor.conditionOperator(),
                        flowElementVisitor.conditionArg2());

                if (conditionResult == Boolean.TRUE)
                {
                    flowToTake = flowElementVisitor.nodeId();
                }
            }

            sequenceFlowIndex++;
        }

        if (flowToTake == NO_FLOW_ID)
        {
            flowToTake = defaultFlowId;
        }

        return flowToTake;
    }

    protected void takeSequenceFlow(BpmnFlowElementEventReader gatewayEvent, FlowElementVisitor sequenceFlow, LogWriters logWriters)
    {
        final DirectBuffer stringIdBuffer = sequenceFlow.stringIdBuffer();

        eventWriter
            .bpmnBranchKey(gatewayEvent.bpmnBranchKey())
            .eventType(ExecutionEventType.SQF_EXECUTED)
            .flowElementId(sequenceFlow.nodeId())
            .flowElementIdString(stringIdBuffer, 0, stringIdBuffer.capacity())
            .processId(gatewayEvent.wfDefinitionId())
            .workflowInstanceId(gatewayEvent.wfInstanceId());

        logWriters.writeToCurrentLog(eventWriter);

    }

    protected void initJsonDocument(long bpmnBranchKey)
    {
        final long branchPosition = eventIndex.get(bpmnBranchKey, -1L);
        logReader.seek(branchPosition);
        logReader.next().readValue(bpmnBranchEventReader);

        final DirectBuffer payload = bpmnBranchEventReader.materializedPayload();
        jsonDocument.wrap(payload, 0, payload.capacity());
    }

    /**
     * @return true if condition is valid and evaluates to true, false if condition is valid and evaluates to false, null if condition is not valid
     */
    protected Boolean evaluateCondition(JsonDocument json, JsonPropertyReader arg1, ConditionOperator comparisonOperator, JsonPropertyReader arg2)
    {
        final JsonScalarReader arg1Value = resolveToScalar(json, arg1);
        final JsonScalarReader arg2Value = resolveToScalar(json, arg2);

        if (arg1Value == null || arg2Value == null)
        {
            return null;
        }
        else
        {
            final boolean comparisonFulfilled;
            switch (comparisonOperator)
            {
                case EQUAL:
                    comparisonFulfilled = EQUAL_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case NOT_EQUAL:
                    comparisonFulfilled = !EQUAL_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case GREATER_THAN:
                    comparisonFulfilled = GREATER_THAN_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case GREATER_THAN_OR_EQUAL:
                    comparisonFulfilled = !LOWER_THAN_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case LOWER_THAN:
                    comparisonFulfilled = LOWER_THAN_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                case LOWER_THAN_OR_EQUAL:
                    comparisonFulfilled = !GREATER_THAN_OPERATOR.apply(arg1Value, arg2Value);
                    break;
                default:
                    comparisonFulfilled = false;
                    break;
            }

            // avoiding auto-boxing. We cannot return primitive boolean as we need three-value-logic here
            if (comparisonFulfilled)
            {
                return Boolean.TRUE;
            }
            else
            {
                return Boolean.FALSE;
            }
        }

    }

    protected JsonScalarReader resolveToScalar(JsonDocument json, JsonPropertyReader jsonProperty)
    {
        if (jsonProperty.type() == JsonType.EXPRESSION)
        {
            return resolveJsonPathToScalar(json, jsonProperty.valueExpression());
        }
        else
        {
            return jsonProperty;
        }
    }

    protected JsonScalarReader resolveJsonPathToScalar(JsonDocument json, DirectBuffer jsonPathExpression)
    {
        final JsonPathResult jsonPathResult = json.jsonPath(jsonPathExpression, 0, jsonPathExpression.capacity());
        if (jsonPathResult.hasResolved())
        {
            if (jsonPathResult.isArray() || jsonPathResult.isObject())
            {
                System.err.println("Sequence flow " + flowElementVisitor.stringId() + ": json path did not resolve to a primitive value (String, Number, Boolean, null)");
                return null;
            }
            else
            {
                return jsonPathResult;
            }
        }
        else
        {
            System.err.println("Sequence flow " + flowElementVisitor.stringId() + ": json path did not resolve");
            return null;
        }
    }

    protected static class EqualOperator implements BooleanBiFunction<JsonScalarReader>
    {

        @Override
        public boolean apply(JsonScalarReader o1, JsonScalarReader o2)
        {
            if (o1.isBoolean() && o2.isBoolean())
            {
                return o1.asBoolean() == o2.asBoolean();
            }
            else if (o1.isNumber() && o2.isNumber())
            {
                return o1.asNumber() == o2.asNumber();
            }
            else if (o1.isString() && o2.isString())
            {
                return BufferUtil.contentsEqual(o1.asEncodedString(), o2.asEncodedString());
            }
            else
            {
                return o1.isNull() && o2.isNull();
            }
        }
    }

    protected static class GreaterThanOperator implements BooleanBiFunction<JsonScalarReader>
    {
        @Override
        public boolean apply(JsonScalarReader arg1, JsonScalarReader arg2)
        {
            // TODO: could also lexicographically compare strings, but that may not be trivial based
            //   on the encoded byte arrays
            return arg1.isNumber() && arg2.isNumber() && arg1.asNumber() > arg2.asNumber();
        }
    }

    protected static class LowerThanOperator implements BooleanBiFunction<JsonScalarReader>
    {
        @Override
        public boolean apply(JsonScalarReader arg1, JsonScalarReader arg2)
        {
            // TODO: could also lexicographically compare strings, but that may not be trivial based
            //   on the encoded byte arrays
            return arg1.isNumber() && arg2.isNumber() && arg1.asNumber() < arg2.asNumber();
        }
    }

    protected interface BooleanBiFunction<T>
    {
        boolean apply(T arg1, T arg2);
    }

}
