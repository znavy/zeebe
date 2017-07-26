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

import io.zeebe.client.event.IncidentEvent;
import io.zeebe.client.event.IncidentEventHandler;
import io.zeebe.client.event.RaftEvent;
import io.zeebe.client.event.RaftEventHandler;
import io.zeebe.client.event.TaskEventHandler;
import io.zeebe.client.event.EventHandler;
import io.zeebe.client.event.EventType;
import io.zeebe.client.event.EventSubscription;
import io.zeebe.client.event.EventSubscriptionBuilder;
import io.zeebe.client.event.WorkflowInstanceEventHandler;
import io.zeebe.client.impl.data.MsgPackMapper;
import io.zeebe.client.impl.task.subscription.EventAcquisition;
import io.zeebe.util.EnsureUtil;

public class TopicSubscriptionBuilderImpl implements EventSubscriptionBuilder
{
    protected EventHandler defaultEventHandler;
    protected TaskEventHandler taskEventHandler;
    protected WorkflowInstanceEventHandler wfEventHandler;
    protected IncidentEventHandler incidentEventHandler;
    protected RaftEventHandler raftEventHandler;

    protected final TopicSubscriptionImplBuilder builder;
    protected final MsgPackMapper msgPackMapper;

    public TopicSubscriptionBuilderImpl(
            TopicClientImpl client,
            EventAcquisition<TopicSubscriptionImpl> acquisition,
            MsgPackMapper msgPackMapper,
            int prefetchCapacity)
    {
        builder = new TopicSubscriptionImplBuilder(client, acquisition, prefetchCapacity);
        this.msgPackMapper = msgPackMapper;
    }

    @Override
    public EventSubscriptionBuilder handler(EventHandler handler)
    {
        this.defaultEventHandler = handler;
        return this;
    }

    @Override
    public EventSubscriptionBuilder taskEventHandler(TaskEventHandler handler)
    {
        this.taskEventHandler = handler;
        return this;
    }

    @Override
    public EventSubscriptionBuilder workflowInstanceEventHandler(WorkflowInstanceEventHandler handler)
    {
        this.wfEventHandler = handler;
        return this;
    }

    @Override
    public EventSubscriptionBuilder incidentEventHandler(IncidentEventHandler handler)
    {
        this.incidentEventHandler = handler;
        return this;
    }

    @Override
    public TopicSubscriptionBuilderImpl raftEventHandler(final RaftEventHandler raftEventHandler)
    {
        this.raftEventHandler = raftEventHandler;
        return this;
    }

    @Override
    public EventSubscription open()
    {
        EnsureUtil.ensureNotNull("name", builder.getName());
        if (defaultEventHandler == null && taskEventHandler == null && wfEventHandler == null && incidentEventHandler == null && raftEventHandler == null)
        {
            throw new RuntimeException("at least one handler must be set");
        }

        builder.handler(this::dispatchEvent);

        final TopicSubscriptionImpl subscription = builder.build();
        subscription.open();
        return subscription;
    }

    protected void dispatchEvent(TopicEventImpl event) throws Exception
    {
        final EventType eventType = event.getEventType();

        if (EventType.TASK == eventType && taskEventHandler != null)
        {
            final TaskEventImpl taskEvent = msgPackMapper.convert(event.getAsMsgPack(), TaskEventImpl.class);
            taskEventHandler.handle(event, taskEvent);
        }
        else if (EventType.WORKFLOW_INSTANCE == eventType && wfEventHandler != null)
        {
            final WorkflowInstanceEventImpl wfEvent = msgPackMapper.convert(event.getAsMsgPack(), WorkflowInstanceEventImpl.class);
            wfEventHandler.handle(event, wfEvent);
        }
        else if (EventType.INCIDENT == eventType && incidentEventHandler != null)
        {
            final IncidentEvent incidentEvent = msgPackMapper.convert(event.getAsMsgPack(), IncidentEventImpl.class);
            incidentEventHandler.handle(event, incidentEvent);
        }
        else if (EventType.RAFT == eventType && raftEventHandler != null)
        {
            final RaftEvent raftEvent = msgPackMapper.convert(event.getAsMsgPack(), RaftEventImpl.class);
            raftEventHandler.handle(event, raftEvent);
        }
        else if (defaultEventHandler != null)
        {
            defaultEventHandler.handle(event, event);
        }
    }

    @Override
    public EventSubscriptionBuilder startAtPosition(long position)
    {
        builder.startPosition(position);
        return this;
    }

    @Override
    public EventSubscriptionBuilder startAtTailOfTopic()
    {
        builder.startAtTailOfTopic();
        return this;
    }

    @Override
    public EventSubscriptionBuilder startAtHeadOfTopic()
    {
        builder.startAtHeadOfTopic();
        return this;
    }

    @Override
    public EventSubscriptionBuilder name(String name)
    {
        builder.name(name);
        return this;
    }

    @Override
    public EventSubscriptionBuilder forcedStart()
    {
        builder.forceStart();
        return this;
    }
}
