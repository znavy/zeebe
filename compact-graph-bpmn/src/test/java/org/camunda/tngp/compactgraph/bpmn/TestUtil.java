package org.camunda.tngp.compactgraph.bpmn;

import org.camunda.tngp.bpmn.graph.FlowElementVisitor;
import org.camunda.tngp.bpmn.graph.ProcessGraph;

public class TestUtil
{

    public static int nodeIdByStringId(ProcessGraph processGraph, String stringId)
    {
        if (stringId == null)
        {
            throw new IllegalArgumentException("Parameter stringId must not be null");
        }

        final FlowElementVisitor flowElementVisitor = new FlowElementVisitor().init(processGraph);
        final int nodeCount = processGraph.nodeCount();

        int nodeId = -1;

        for (int i = 0; i < nodeCount; i++)
        {
            flowElementVisitor.moveToNode(i);

            if (stringId.equals(flowElementVisitor.stringId()))
            {
                nodeId = i;
                break;
            }
        }

        return nodeId;
    }

}
