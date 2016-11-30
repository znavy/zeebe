package org.camunda.tngp.bpmn.graph.transformer;

import java.util.Collection;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.tngp.bpmn.graph.ProcessGraph;

public class BpmnModelInstanceTransformer
{
    public ProcessGraph transformSingleProcess(BpmnModelInstance model, long id)
    {
        final Collection<Process> processes = model.getModelElementsByType(Process.class);
        if (processes.size() != 1)
        {
            throw new IllegalArgumentException("Model instance must contain exactly one executable process.");
        }

        final Process process = processes.iterator().next();
        if (!process.isExecutable())
        {
            throw new IllegalArgumentException("Model instance must contain exactly one executable process.");
        }

        return new BpmnProcessGraphTransformer(process, id).transform();
    }
}
