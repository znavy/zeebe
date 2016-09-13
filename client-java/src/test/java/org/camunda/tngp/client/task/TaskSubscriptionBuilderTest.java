package org.camunda.tngp.client.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.camunda.tngp.client.task.impl.PollableTaskSubscriptionBuilderImpl;
import org.camunda.tngp.client.task.impl.TaskAcquisition;
import org.camunda.tngp.client.task.impl.TaskSubscriptionBuilderImpl;
import org.camunda.tngp.client.task.impl.TaskSubscriptionImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TaskSubscriptionBuilderTest
{

    @Mock
    protected TaskAcquisition acquisition;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldBuildSubscription()
    {
        // given
        final TaskSubscriptionBuilder builder = new TaskSubscriptionBuilderImpl(acquisition);

        final TaskHandler handler = mock(TaskHandler.class);
        builder
            .handler(handler)
            .lockTime(654L)
            .taskQueueId(123)
            .taskType("fooo");

        // when
        final TaskSubscription taskSubscription = builder.open();

        // then
        assertThat(taskSubscription instanceof TaskSubscriptionImpl);

        final TaskSubscriptionImpl subscriptionImpl = (TaskSubscriptionImpl) taskSubscription;
        assertThat(subscriptionImpl.getLockTime()).isEqualTo(654L);
        assertThat(subscriptionImpl.getMaxTasks()).isEqualTo(1);
        assertThat(subscriptionImpl.getTaskQueueId()).isEqualTo(123);
        assertThat(subscriptionImpl.getTaskType()).isEqualTo("fooo");

        verify(acquisition).openSubscription(subscriptionImpl);
    }

    @Test
    public void shouldBuildPollableSubscription()
    {
        // given
        final PollableTaskSubscriptionBuilder builder = new PollableTaskSubscriptionBuilderImpl(acquisition);

        builder
            .lockTime(654L)
            .taskQueueId(123)
            .taskType("fooo");

        // when
        final PollableTaskSubscription taskSubscription = builder.open();

        // then
        assertThat(taskSubscription instanceof TaskSubscriptionImpl);

        final TaskSubscriptionImpl subscriptionImpl = (TaskSubscriptionImpl) taskSubscription;
        assertThat(subscriptionImpl.getLockTime()).isEqualTo(654L);
        assertThat(subscriptionImpl.getMaxTasks()).isEqualTo(1);
        assertThat(subscriptionImpl.getTaskQueueId()).isEqualTo(123);
        assertThat(subscriptionImpl.getTaskType()).isEqualTo("fooo");

        verify(acquisition).openSubscription(subscriptionImpl);
    }

}
