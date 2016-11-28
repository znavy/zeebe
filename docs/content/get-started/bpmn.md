---

title: "BPMN"
weight: 30

menu:
  main:
    identifier: "bpmn"
    parent: "get-started"

---


# Supported Elements

* None Start Event
* None End Event
* Service Task

# BPMN Extensions

In addition to the standard BPMN attributes, Camunda Tngp requires the BPMN XML to contain additional attributes.

## Service Task

* `camunda:type`: The type of task that identifies *which* task must be performed.
* `camunda:taskQueueId`: The id of the task queue the tasks are added to. The value must correspond to the `id` value of a `[task-queue]` element in the broker configuration.

## Sequence Flow

Conditions are supported for sequence flows leaving exclusive gateways. The condition must be defined as custom attributes of the `condition` child element.

### Condition

* `camunda:arg1`: First argument of the condition expression. Can be a JSON path expression, JSON String, Number, Boolean or `null` literal. In case of a JSON path expression, the expression is resolved at runtime against the scope's JSON payload. JSON String values must be quoted.
* `camunda:arg2`: Second argument of the expression. Same restrictions apply as for `arg1`.
* `camunda:operator`: Binary operator that is being applied to `arg1` and `arg2` that determines if the condition is fulfilled. Valid values are `EQUAL`, `NOT_EQUAL`, `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LOWER_THAN`, `LOWER_THAN_OR_EQUAL`.

Example:

```xml
<sequenceFlow id="flow1" sourceRef="exclusiveGateway" targetRef="serviceTask1">
  <conditionExpression camunda:arg1="$.path.to.value" camunda:arg2="1" camunda:operator="EQUAL" id="conditionExpression_e1355144-7887-4119-b552-5b9519535348"/>
</sequenceFlow>
```