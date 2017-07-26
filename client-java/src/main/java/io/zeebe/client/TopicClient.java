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

import io.zeebe.client.event.EventSubscriptionBuilder;
import io.zeebe.client.event.PollableEventSubscriptionBuilder;
import io.zeebe.client.task.*;
import io.zeebe.client.task.cmd.*;
import io.zeebe.client.workflow.cmd.*;

public interface TopicClient
{
    /**
     * Deploy new workflow definitions.
     */
    CreateDeploymentCmd deploy();

    /**
     * Create new workflow instance.
     */
    StartWorkflowInstanceCmd startWorkflowInstance();

    /**
     * Cancel a workflow instance.
     */
    CancelWorkflowInstanceCmd cancelWorkflowInstance();

    /**
     * Update the payload of a workflow instance.
     */
    UpdateWorkflowPayloadCmd updateWorkflowPayload();

    /**
     * Create a new task.
     */
    CreateTaskCmd createTask();

    /**
     * Complete a locked task.
     */
    CompleteTaskCmd completeTask();

    /**
     * Mark a locked task as failed.
     */
    FailTaskCmd failTask();

    /**
     * Update the remaining retries of a task.
     */
    UpdateTaskRetriesCmd updateTaskRetries();

    /**
     * Create a new subscription to lock tasks and execute them by the given
     * handler.
     */
    TaskSubscriptionBuilder newTaskSubscription();

    /**
     * Create a new subscription to lock tasks. Use
     * {@linkplain PollableTaskSubscription#poll(io.zeebe.client.task.TaskHandler)}
     * to execute the locked tasks.
     */
    PollableTaskSubscriptionBuilder newPollableTaskSubscription();

    /**
     * @return a builder for an event subscription and managed event handling
     */
    EventSubscriptionBuilder newEventSubscription();

    /**
     * @return a builder for an event subscription and manual event handling
     */
    PollableEventSubscriptionBuilder newPollableEventSubscription();
}
