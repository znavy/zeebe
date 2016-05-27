package org.camunda.tngp.bpmn.graph.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.tngp.bpmn.graph.ProcessGraph;

public class BpmnModelInstanceTransformer
{
    public List<ProcessGraph> transpformAll(BpmnModelInstance model)
    {
        final Collection<Process> processes = model.getModelElementsByType(Process.class);
        final List<ProcessGraph> transformedProcesses = new ArrayList<>(processes.size());

        for (Process process : processes)
        {
            if(process.isExecutable())
            {
                transformedProcesses.add(new BpmnProcessGraphTransformer(process).transform());
            }
        }

        return transformedProcesses;

    }

    public ProcessGraph transformSingleProcess(BpmnModelInstance model)
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
