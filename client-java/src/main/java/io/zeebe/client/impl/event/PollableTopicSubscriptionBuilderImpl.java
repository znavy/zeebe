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

import io.zeebe.client.event.PollableEventSubscription;
import io.zeebe.client.event.PollableEventSubscriptionBuilder;
import io.zeebe.client.impl.task.subscription.EventAcquisition;
import io.zeebe.util.EnsureUtil;

public class PollableTopicSubscriptionBuilderImpl implements PollableEventSubscriptionBuilder
{
    protected TopicSubscriptionImplBuilder implBuilder;

    public PollableTopicSubscriptionBuilderImpl(
            TopicClientImpl client,
            EventAcquisition<TopicSubscriptionImpl> acquisition,
            int prefetchCapacity)
    {
        implBuilder = new TopicSubscriptionImplBuilder(client, acquisition, prefetchCapacity);
    }

    public PollableTopicSubscriptionBuilderImpl(TopicClientImpl topicClientImpl)
    {
    }

    @Override
    public PollableEventSubscription open()
    {
        EnsureUtil.ensureNotNull("name", implBuilder.getName());

        final TopicSubscriptionImpl subscription = implBuilder.build();
        subscription.open();
        return subscription;
    }

    @Override
    public PollableEventSubscriptionBuilder startAtPosition(long position)
    {
        implBuilder.startPosition(position);
        return this;
    }

    @Override
    public PollableEventSubscriptionBuilder startAtTailOfTopic()
    {
        implBuilder.startAtTailOfTopic();
        return this;
    }

    @Override
    public PollableEventSubscriptionBuilder startAtHeadOfTopic()
    {
        implBuilder.startAtHeadOfTopic();
        return this;
    }

    @Override
    public PollableEventSubscriptionBuilder name(String subscriptionName)
    {
        implBuilder.name(subscriptionName);
        return this;
    }

    @Override
    public PollableEventSubscriptionBuilder forcedStart()
    {
        implBuilder.forceStart();
        return this;
    }

}
