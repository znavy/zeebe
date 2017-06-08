package org.camunda.tngp.broker.it.management;

import org.camunda.tngp.broker.it.ClientRule2;
import org.camunda.tngp.broker.it.EmbeddedBrokerRule2;
import org.camunda.tngp.client.TngpClient;
import org.camunda.tngp.client.management.TopicManagementEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;

public class TopicsManagementTest
{
    public EmbeddedBrokerRule2 brokerRule = new EmbeddedBrokerRule2();

    public ClientRule2 clientRule = new ClientRule2();

    @Rule
    public RuleChain ruleChain = RuleChain
        .outerRule(brokerRule)
        .around(clientRule);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

//    @Rule
//    public Timeout testTimeout = Timeout.seconds(10);


    @Test
    public void testCreateTopic() throws InterruptedException
    {
        final TngpClient client = clientRule.getClient();

        Thread.sleep(2000);

        final TopicManagementEvent evt = client.management()
            .createTopic()
            .topicName("foo")
            .partitionCount(16)
            .execute();

        System.out.println(evt.getEventType());

        Thread.sleep(2000);

        final TopicManagementEvent deleteEvt = client.management()
            .deleteTopic()
            .topicName("foo")
            .partitionCount(16)
            .execute();

        System.out.println(deleteEvt.getEventType());

        client.topic("zb-topics-management", 0)
            .newSubscription()
            .name("foo")
            .startAtHeadOfTopic()
            .handler((m, e) ->
            {
                System.out.println(e.getJson());
            })
            .open();

        Thread.sleep(5000);
    }

}
