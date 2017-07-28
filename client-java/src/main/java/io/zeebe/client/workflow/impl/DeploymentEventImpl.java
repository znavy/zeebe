/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client.workflow.impl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.zeebe.client.event.TopicEventType;
import io.zeebe.client.event.impl.EventImpl;
import io.zeebe.client.workflow.cmd.DeploymentEvent;
import io.zeebe.client.workflow.cmd.WorkflowDefinition;

public class DeploymentEventImpl extends EventImpl implements DeploymentEvent
{
    private byte[] bpmnXml;

    private List<WorkflowDefinition> deployedWorkflows;

    private String errorMessage;

    @JsonCreator
    public DeploymentEventImpl(@JsonProperty("state") String state)
    {
        super(TopicEventType.DEPLOYMENT, state);
    }

    public byte[] getBpmnXml()
    {
        return bpmnXml;
    }

    public void setBpmnXml(byte[] bpmnXml)
    {
        this.bpmnXml = bpmnXml;
    }

    public List<WorkflowDefinition> getDeployedWorkflows()
    {
        return deployedWorkflows;
    }

    public void setDeployedWorkflows(List<WorkflowDefinition> deployedWorkflows)
    {
        this.deployedWorkflows = deployedWorkflows;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }
}
