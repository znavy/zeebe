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
package io.zeebe.client.impl.event;

import io.zeebe.client.TopicClient;
import io.zeebe.client.event.PollableEventSubscriptionBuilder;
import io.zeebe.client.event.EventSubscriptionBuilder;
import io.zeebe.client.impl.ZeebeClientImpl;
import io.zeebe.client.impl.task.*;
import io.zeebe.client.impl.task.subscription.TaskSubscriptionBuilderImpl;
import io.zeebe.client.impl.workflow.*;
import io.zeebe.client.task.PollableTaskSubscriptionBuilder;
import io.zeebe.client.task.TaskSubscriptionBuilder;
import io.zeebe.client.task.cmd.*;
import io.zeebe.client.workflow.cmd.*;

public class TopicClientImpl implements TopicClient
{
    protected final ZeebeClientImpl client;
    protected final String topicName;

    public TopicClientImpl(final ZeebeClientImpl client, final String topicName)
    {
        this.client = client;
        this.topicName = topicName;
    }

    @Override
    public EventSubscriptionBuilder newEventSubscription()
    {
        return client.getSubscriptionManager().newTopicSubscription(this);
    }

    @Override
    public PollableEventSubscriptionBuilder newPollableEventSubscription()
    {
        return client.getSubscriptionManager().newPollableTopicSubscription(this);
    }

    public String getTopicName()
    {
        return topicName;
    }

    public ZeebeClientImpl getClient()
    {
        return client;
    }

    @Override
    public CreateDeploymentCmd deploy()
    {
        return new CreateDeploymentCmdImpl(this);
    }

    @Override
    public StartWorkflowInstanceCmd startWorkflowInstance()
    {
        return new StartWorkflowInstanceCmdImpl(this);
    }

    @Override
    public CancelWorkflowInstanceCmd cancelWorkflowInstance()
    {
        return new CancelWorkflowInstanceCmdImpl(this);
    }

    @Override
    public UpdateWorkflowPayloadCmd updateWorkflowPayload()
    {
        return new UpdateWorkflowPayloadCmdImpl(this);
    }

    @Override
    public CreateTaskCmd createTask()
    {
        return new CreateTaskCmdImpl(this);
    }

    @Override
    public CompleteTaskCmd completeTask()
    {
        return new CompleteTaskCmdImpl(this);
    }

    @Override
    public FailTaskCmd failTask()
    {
        return new FailTaskCmdImpl(this);
    }

    @Override
    public UpdateTaskRetriesCmd updateTaskRetries()
    {
        return new UpdateTaskRetriesCmdImpl(this);
    }

    @Override
    public TaskSubscriptionBuilder newTaskSubscription()
    {
        return new TaskSubscriptionBuilderImpl(this);
    }

    @Override
    public PollableTaskSubscriptionBuilder newPollableTaskSubscription()
    {
        return new PollableTopicSubscriptionBuilderImpl(this);
    }
}
