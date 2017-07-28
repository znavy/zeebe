package io.zeebe.client.workflow.cmd;

import java.util.List;

import io.zeebe.client.event.Event;

public interface DeploymentEvent extends Event
{
    // TODO: javadoc

    byte[] getBpmnXml();
    List<WorkflowDefinition> getDeployedWorkflows();
    String getErrorMessage();
}
