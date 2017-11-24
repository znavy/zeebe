package io.zeebe.broker.it.subscription;

import static io.zeebe.test.util.TestUtil.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.zeebe.broker.Broker;
import io.zeebe.broker.clustering.ClusterServiceNames;
import io.zeebe.broker.it.ClientRule;
import io.zeebe.broker.it.clustering.Brokers;
import io.zeebe.broker.it.clustering.TopologyObserver;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.event.TopicSubscription;
import io.zeebe.client.impl.Loggers;
import io.zeebe.raft.Raft;
import io.zeebe.servicecontainer.impl.ServiceContainerImpl;
import io.zeebe.test.util.AutoCloseableRule;
import io.zeebe.transport.SocketAddress;

public class TopicSubscriptionClusteredTest
{

    @Rule
    public ClientRule clientRule = new ClientRule(false);

    @Rule
    public AutoCloseableRule closeables = new AutoCloseableRule();

    protected Brokers brokers;

    @Before
    public void setUp()
    {
        brokers = new Brokers();
        closeables.manage(brokers);
    }

    @Test
    public void shouldReopenSubscriptionOnLeaderChangeInCaseOfShutdown()
    {
        brokers.startBroker(Brokers.BROKER_1_CLIENT_ADDRESS, Brokers.BROKER_1_TOML);
        brokers.startBroker(Brokers.BROKER_2_CLIENT_ADDRESS, Brokers.BROKER_2_TOML);
        brokers.startBroker(Brokers.BROKER_3_CLIENT_ADDRESS, Brokers.BROKER_3_TOML);

        final ZeebeClient client = clientRule.getClient();
        final TopologyObserver observer = new TopologyObserver(client);

        // => https://github.com/zeebe-io/zeebe/issues/534
        observer.waitForBrokers(brokers.getBrokerAddresses());

        final String topicName = "foo";
        client.topics().create(topicName, 1).execute();

        final TaskEvent taskEvent = client.tasks().create(topicName, "bar").execute();
        final int partitionId = taskEvent.getMetadata().getPartitionId();

        final SocketAddress currentLeaderAddress = observer.waitForLeader(partitionId);

        final RecordingEventHandler handler = new RecordingEventHandler();
        final TopicSubscription subscription = client.topics()
                .newSubscription(topicName)
                .name("sub")
                .handler(handler)
                .startAtHeadOfTopic()
                .open();
        brokers.close(currentLeaderAddress);

        observer.waitForLeader(partitionId);

        // when
        Loggers.CLIENT_LOGGER.debug("Starting task request");
        client.tasks().create(topicName, "baz").execute();

        // then
        waitUntil(() -> subscription.isOpen());
        assertThat(subscription.isOpen()).isTrue();
        waitUntil(() -> handler.numRecordedTaskEvents() >= 4);
        assertThat(handler.numRecordedTaskEvents()).isGreaterThanOrEqualTo(4);
        // TODO: should assert that the events after leader change were received

        // TODO: this test works as is, because when the leader is shut down, the client connection is terminated
        // and that causes the client to reconnect
    }

    @Test
    public void shouldReopenSubscriptionOnLeaderChangeInFlight()
    {
        // given
        brokers.startBroker(Brokers.BROKER_1_CLIENT_ADDRESS, Brokers.BROKER_1_TOML);
        brokers.startBroker(Brokers.BROKER_2_CLIENT_ADDRESS, Brokers.BROKER_2_TOML);
        brokers.startBroker(Brokers.BROKER_3_CLIENT_ADDRESS, Brokers.BROKER_3_TOML);

        final ZeebeClient client = clientRule.getClient();
        final TopologyObserver observer = new TopologyObserver(client);

        // => https://github.com/zeebe-io/zeebe/issues/534
        observer.waitForBrokers(brokers.getBrokerAddresses());

        final String topicName = "foo";
        client.topics().create(topicName, 1).execute();

        final TaskEvent taskEvent = client.tasks().create(topicName, "bar").execute();
        final int partitionId = taskEvent.getMetadata().getPartitionId();

        final SocketAddress firstLeader = observer.waitForLeader(partitionId);

        final RecordingEventHandler handler = new RecordingEventHandler();
        final TopicSubscription subscription = client.topics().newSubscription(topicName).name("sub").handler(handler).open();

        // workaround for https://github.com/zeebe-io/zeebe/issues/200 (i.e. make sure the request has been processed)
        client.tasks().create(topicName, "baz").execute();
        waitUntil(() -> handler.numRecordedTaskEvents() > 0);

        Loggers.CLIENT_LOGGER.debug("Forcing leader change");
        forceLeaderChange(brokers, observer, topicName, partitionId);

        final Set<SocketAddress> potentialNewLeaders = brokers.getBrokerAddresses();
        potentialNewLeaders.remove(firstLeader);
        observer.waitForLeader(partitionId, potentialNewLeaders);

        // when
        Loggers.CLIENT_LOGGER.debug("Starting task request");
        client.tasks().create(topicName, "baz").execute();

        // then
        waitUntil(() -> subscription.isOpen());
        assertThat(subscription.isOpen()).isTrue();
        waitUntil(() -> handler.numRecordedTaskEvents() >= 4);
        assertThat(handler.numRecordedTaskEvents()).isGreaterThanOrEqualTo(4);
        // TODO: should assert that the events after leader change were received

        // TODO: remaining issue for instability is https://github.com/zeebe-io/zeebe/issues/578:
        // das subscribe request on reopen may remain unanswered
    }

    // TODO: test the same things with task subscriptions

    // TODO: this is not robust enough to ensure that another broker becomes leader;
    // it can happent that the new candidate does not become leader (not enough votes) and then the old leader may become the new leader
    protected void forceLeaderChange(Brokers brokers, TopologyObserver observer, String topic, int partition)
    {
        final SocketAddress leaderAddress = observer.waitForLeader(partition);
        final Set<SocketAddress> brokerAddresses = brokers.getBrokerAddresses();
        brokerAddresses.remove(leaderAddress);
        final SocketAddress anyFollower = brokerAddresses.iterator().next();

        final Broker follower = brokers.get(anyFollower);
        final Raft raft = getRaft(follower, topic, partition);
        raft.becomeCandidateAsync();
    }

    protected final Raft getRaft(Broker broker, String topic, int partition)
    {
        final ServiceContainerImpl serviceContainer = (ServiceContainerImpl) broker.getBrokerContext().getServiceContainer();
        final String logStreamName = topic + "." + partition;

        final Raft raft = serviceContainer.<Raft>getService(ClusterServiceNames.raftServiceName(logStreamName)).get();
        return raft;
    }

    @After
    public void tearDown()
    {
        Loggers.CLIENT_LOGGER.debug("tear down");
    }
}
