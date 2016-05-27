package org.camunda.tngp.bpmn.graph;

public class BpmnEdgeTypes
{
    public final static int NODE_OUTGOING_SEQUENCE_FLOWS = 0;
    public final static int NODE_INCOMMING_SEQUENCE_FLOWS = 1;
    public final static int NODE_PARENT = 2;
    public final static int NODE_CHILD_NODES = 3;

    public final static int SEQUENCE_FLOW_SOURCE_NODE = 4;
    public final static int SEQUENCE_FLOW_TARGET_NODE = 5;

    public final static int EDGE_TYPE_COUNT = 6;
}
