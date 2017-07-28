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
package io.zeebe.client;

import io.zeebe.client.event.WorkflowInstanceEvent;
import io.zeebe.client.task.cmd.Request;
import io.zeebe.client.workflow.cmd.CreateDeploymentCommand;
import io.zeebe.client.workflow.cmd.CreateWorkflowInstanceCommand;
import io.zeebe.client.workflow.cmd.UpdatePayloadCommand;

public interface WorkflowsClient
{

    /**
     * Deploy new workflow definitions.
     */
    CreateDeploymentCommand deploy(String topic);

    /**
     * Create new workflow instance.
     */
    CreateWorkflowInstanceCommand create(String topic);

    /**
     * Cancel a workflow instance.
     */
    Request<WorkflowInstanceEvent> cancel(WorkflowInstanceEvent baseEvent);

    /**
     * Update the payload of a workflow instance.
     */
    // TODO: extend javadoc and make it clear that the base event should be an event of a currently active(??) activity instance
    UpdatePayloadCommand updatePayload(WorkflowInstanceEvent baseEvent);
}
