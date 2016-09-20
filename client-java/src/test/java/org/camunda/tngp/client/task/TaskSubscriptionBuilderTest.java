package org.camunda.tngp.client.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.camunda.tngp.client.task.impl.PollableTaskSubscriptionBuilderImpl;
import org.camunda.tngp.client.task.impl.TaskAcquisition;
import org.camunda.tngp.client.task.impl.TaskSubscriptionBuilderImpl;
import org.camunda.tngp.client.task.impl.TaskSubscriptionImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TaskSubscriptionBuilderTest
{

    @Mock
    protected TaskAcquisition acquisition;

    @Rule
    public ExpectedException exception = ExpectedException.none();

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

    @Test
    public void shouldValidateMissingTaskType()
    {
        // given
        final PollableTaskSubscriptionBuilder builder = new PollableTaskSubscriptionBuilderImpl(acquisition);

        builder
            .lockTime(654L)
            .taskQueueId(123);

        // then
        exception.expect(RuntimeException.class);
        exception.expectMessage("taskType must not be null");

        // when
        builder.open();
    }

    @Test
    public void shouldValidateMissingTaskHandler()
    {
        // given
        final TaskSubscriptionBuilder builder = new TaskSubscriptionBuilderImpl(acquisition);

        builder
            .lockTime(654L)
            .taskQueueId(123)
            .taskType("foo");

        // then
        exception.expect(RuntimeException.class);
        exception.expectMessage("taskHandler must not be null");

        // when
        builder.open();
    }

    @Test
    public void shouldValidateLockTime()
    {
        // given
        final TaskSubscriptionBuilder builder = new TaskSubscriptionBuilderImpl(acquisition);

        builder
            .lockTime(0L)
            .taskQueueId(123)
            .taskType("foo")
            .handler((t) ->
            { });

        // then
        exception.expect(RuntimeException.class);
        exception.expectMessage("lockTime must be greater than 0");

        // when
        builder.open();
    }
}
