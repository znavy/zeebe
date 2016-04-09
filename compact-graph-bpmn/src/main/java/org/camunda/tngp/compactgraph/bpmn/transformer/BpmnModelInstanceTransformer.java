package org.camunda.tngp.compactgraph.bpmn.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.compactgraph.bpmn.BpmnProcessGraph;
import org.camunda.bpm.model.bpmn.instance.Process;

public class BpmnModelInstanceTransformer
{
    public List<BpmnProcessGraph> transpformAll(BpmnModelInstance model)
    {
        final Collection<Process> processes = model.getModelElementsByType(Process.class);
        final List<BpmnProcessGraph> transformedProcesses = new ArrayList<>(processes.size());

        for (Process process : processes)
        {
            if(process.isExecutable())
            {
                transformedProcesses.add(new BpmnProcessGraphTransformer(process).transform());
            }
        }

        return transformedProcesses;

    }

    public BpmnProcessGraph transpformSingleProcess(BpmnModelInstance model)
    {
        final Collection<Process> processes = model.getModelElementsByType(Process.class);
        if(processes.size() != 1)
        {
            throw new IllegalArgumentException("Model instance must contain exactly one executable process.");
        }
        Process process = processes.iterator().next();
        if(!process.isExecutable())
        {
            throw new IllegalArgumentException("Model instance must contain exactly one executable process.");
        }

        return new BpmnProcessGraphTransformer(process).transform();
    }
}
