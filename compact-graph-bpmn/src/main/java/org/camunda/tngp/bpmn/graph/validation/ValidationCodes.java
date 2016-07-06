package org.camunda.tngp.bpmn.graph.validation;

public class ValidationCodes
{
    public static final int PROCESS_MULTIPLE_START_EVENTS = 100;
    public static final int PROCESS_NO_START_EVENT = 101;
    public static final int PROCESS_START_EVENT_UNSUPPORTED = 102;

    public static final int SERVICE_TASK_MISSING_TASK_TYPE = 200;
    public static final int SERVICE_TASK_MISSING_TASK_QUEUE_ID = 201;
    public static final int SERVICE_TASK_INVALID_TASK_QUEUE_ID = 202;
}
