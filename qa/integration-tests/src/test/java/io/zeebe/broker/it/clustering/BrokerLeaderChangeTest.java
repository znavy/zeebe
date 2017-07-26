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
package io.zeebe.broker.it.clustering;

import static io.zeebe.logstreams.log.LogStream.DEFAULT_PARTITION_ID;
import static io.zeebe.logstreams.log.LogStream.DEFAULT_TOPIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.*;

import io.zeebe.broker.Broker;
import io.zeebe.broker.it.ClientRule;
import io.zeebe.client.*;
import io.zeebe.client.clustering.impl.TopicLeader;
import io.zeebe.client.impl.Topic;
import io.zeebe.client.task.PollableTaskSubscription;
import io.zeebe.transport.SocketAddress;
import org.junit.*;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerLeaderChangeTest
{
    // TODO: remove logging after test becomes stable
    public static final Logger LOG = LoggerFactory.getLogger(BrokerLeaderChangeTest.class);

    public static final Topic DEFAULT_TOPIC = new Topic(DEFAULT_TOPIC_NAME, DEFAULT_PARTITION_ID);

    public static final String BROKER_1_TOML = "zeebe.cluster.1.cfg.toml";
    public static final SocketAddress BROKER_1_CLIENT_ADDRESS = new SocketAddress("localhost", 51015);
    public static final SocketAddress BROKER_1_RAFT_ADDRESS = new SocketAddress("localhost", 51017);

    public static final String BROKER_2_TOML = "zeebe.cluster.2.cfg.toml";
    public static final SocketAddress BROKER_2_CLIENT_ADDRESS = new SocketAddress("localhost", 41015);
    public static final SocketAddress BROKER_2_RAFT_ADDRESS = new SocketAddress("localhost", 41017);

    public static final String BROKER_3_TOML = "zeebe.cluster.3.cfg.toml";
    public static final SocketAddress BROKER_3_CLIENT_ADDRESS = new SocketAddress("localhost", 31015);
    public static final SocketAddress BROKER_3_RAFT_ADDRESS = new SocketAddress("localhost", 31017);

    public static final String TASK_TYPE = "testTask";

    @Rule
    public ClientRule clientRule = new ClientRule();

    protected final Map<SocketAddress, Broker> brokers = new HashMap<>();

    protected ZeebeClient client;
    protected TopicClient topicClient;
    protected TaskTopicClient taskClient;

    @Rule
    public Timeout testTimeout = Timeout.seconds(120);

    @Before
    public void setUp()
    {
        client = clientRule.getClient();
        topicClient = clientRule.topic();
        taskClient = clientRule.taskTopic();
    }

    @After
    public void tearDown()
    {
        for (final Broker broker : brokers.values())
        {
            broker.close();
        }
    }

    @Test
    public void test() throws Exception
    {
        startBroker(BROKER_1_CLIENT_ADDRESS, BROKER_1_TOML);
        startBroker(BROKER_2_CLIENT_ADDRESS, BROKER_2_TOML);
        startBroker(BROKER_3_CLIENT_ADDRESS, BROKER_3_TOML);

        // create task on leader
        LOG.info("Creating task for type {}", TASK_TYPE);
        final long taskKey = taskClient
            .create()
            .taskType(TASK_TYPE)
            .execute();
        LOG.info("Task created with key {}", taskKey);

        final List<TopicLeader> topicLeaders = client.requestTopology().execute().getTopicLeaders();
        final Optional<TopicLeader> leader = topicLeaders.stream().filter((l) -> DEFAULT_TOPIC.equals(l.getTopic())).findFirst();

        client.disconnect();

        // stop leader
        brokers.remove(leader.get().getSocketAddress()).close();

        // complete task on new leader
        try (PollableTaskSubscription taskSubscription = taskClient.newPollableTaskSubscription()
            .taskType(TASK_TYPE)
            .lockOwner("someLockOwner")
            .open())
        {
            int tasksCompleted = 0;
            do
            {
                tasksCompleted = taskSubscription.poll((t) -> t.complete());
            }
            while (tasksCompleted != 1);
        }

    }

    protected void startBroker(final SocketAddress socketAddress, final String configFilePath)
    {
        LOG.info("starting broker {} with config {}", socketAddress, configFilePath);
        final InputStream config = BrokerLeaderChangeTest.class.getClassLoader().getResourceAsStream(configFilePath);
        assertThat(config).isNotNull();

        brokers.put(socketAddress, new Broker(config));
    }
}
