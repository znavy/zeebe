package io.zeebe.broker.it.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import io.zeebe.broker.it.ClientRule;
import io.zeebe.broker.it.clustering.ClusteringRule;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.clustering.impl.TopologyBroker;
import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.topic.Topic;
import io.zeebe.client.topic.Topics;
import io.zeebe.test.util.AutoCloseableRule;

public class TaskEventClusteredTest
{
    public ClientRule clientRule = new ClientRule(false);
    public AutoCloseableRule closeables = new AutoCloseableRule();
    public ClusteringRule clusteringRule = new ClusteringRule(closeables, clientRule);

    @Rule
    public RuleChain ruleChain =
        RuleChain.outerRule(closeables)
                 .around(clientRule)
                 .around(clusteringRule);

    @Test
    public void shouldCreateTaskWhenFollowerUnavailable()
    {
        // given
        final ZeebeClient client = clientRule.getClient();

        final String topicName = "foo";
        clusteringRule.createTopic(topicName, 1);

        final Topics topics = client.topics().getTopics().execute();
        final Topic topic = topics.getTopics()
            .stream()
            .filter(t -> topicName.equals(t.getName()))
            .findFirst()
            .get();

        final TopologyBroker leader = clusteringRule.getLeaderForPartition(topic.getPartitions().get(0).getId());

        // choosing a new leader in a raft group where the previously leading broker is no longer available
        clusteringRule.stopBroker(leader.getSocketAddress());

        // when
        final TaskEvent taskEvent = client.tasks().create(topicName, "bar").execute();

        // then
        assertThat(taskEvent.getState()).isEqualTo("CREATED");

        fail("Warum ruft der Sender so oft DISCARD auf? (10 mal pro Millisekunde?)");
    }
}
