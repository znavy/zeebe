package io.zeebe.broker.it.clustering;

import static io.zeebe.test.util.TestUtil.doRepeatedly;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.zeebe.broker.Broker;
import io.zeebe.broker.it.ClientRule;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.clustering.impl.TopologyResponse;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.instance.WorkflowDefinition;
import io.zeebe.test.util.AutoCloseableRule;
import io.zeebe.transport.SocketAddress;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

/**
 *
 */
public class GossipClusteringTest
{
    private static final int PARTITION_COUNT = 5;

    @Rule
    public AutoCloseableRule closeables = new AutoCloseableRule();

    @Rule
    public ClientRule clientRule = new ClientRule(false);

    @Rule
    public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ZeebeClient client;
    private List<Broker> brokers;

    @Test
    public void shouldStartCluster()
    {
        // given
        client = clientRule.getClient();
        brokers = new ArrayList<>();

        // when
        brokers.add(startBroker("zeebe.cluster.1.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.2.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.3.cfg.toml"));

        // then wait until cluster is ready
        doRepeatedly(() -> client.requestTopology().execute().getBrokers())
            .until(topologyBroker -> topologyBroker.size() == brokers.size());
    }


    @Test
    public void shouldDistributePartitionsAndLeaderInformationInCluster()
    {
        // given
        client = clientRule.getClient();

        brokers = new ArrayList<>();
        brokers.add(startBroker("zeebe.cluster.1.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.2.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.3.cfg.toml"));

        doRepeatedly(() -> client.requestTopology().execute().getBrokers())
            .until(topologyBroker -> topologyBroker.size() == brokers.size());

        // when
        client.topics().create("test", PARTITION_COUNT).execute();

        // then
        final TopologyResponse topology = client.requestTopology()
                                                .execute();

        assertThat(topology.getBrokers().size()).isEqualTo(brokers.size());
        final long partitionLeaderCount = topology.getTopicLeaders()
                                  .stream()
                                  .filter((leader) -> leader.getTopicName()
                                                            .equals("test"))
                                  .count();
        assertThat(partitionLeaderCount).isEqualTo(PARTITION_COUNT);
    }

    // TODO test if broker dies topology has one broker less

    // TODO test sync - if node comes later to the cluster

    private Broker startBroker(String configFile)
    {
        final InputStream config = this.getClass().getClassLoader().getResourceAsStream(configFile);
        final Broker broker = new Broker(config);
        closeables.manage(broker);

        return broker;
    }
}
