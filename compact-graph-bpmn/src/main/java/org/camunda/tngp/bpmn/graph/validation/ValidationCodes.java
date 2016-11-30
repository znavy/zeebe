package org.camunda.tngp.bpmn.graph.validation;

public class ValidationCodes
{
    public static final int GENERAL_UNSUPPORTED_ELEMENT = 100;

    public static final int PROCESS_MULTIPLE_START_EVENTS = 200;
    public static final int PROCESS_NO_START_EVENT = 201;
    public static final int PROCESS_START_EVENT_UNSUPPORTED = 202;
    public static final int PROCESS_NOT_EXECUTABLE = 203;
    public static final int DEFINITIONS_NOT_SINGLE_PROCESS = 204;

    public static final int SERVICE_TASK_MISSING_TASK_TYPE = 300;
    public static final int SERVICE_TASK_MISSING_TASK_QUEUE_ID = 301;
    public static final int SERVICE_TASK_INVALID_TASK_QUEUE_ID = 302;

    public static final int SEQUENCE_FLOW_UNSUPPORTED_CONDITION = 400;
    public static final int SEQUENCE_FLOW_BPMN_FORBIDDEN_CONDITION = 401;
    public static final int SEQUENCE_FLOW_MISSING_CONDITION = 402;
    public static final int SEQUENCE_FLOW_MISSING_CONDITION_ATTRIBUTE = 403;
    public static final int SEQUENCE_FLOW_INVALID_CONDITION_ATTRIBUTE = 404;

    public static final int EXCLUSIVE_GW_INVALID_FLOWS = 501;

}
