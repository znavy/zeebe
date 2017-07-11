package io.zeebe.client.clustering.impl;

import java.util.*;

import io.zeebe.client.clustering.Topology;
import io.zeebe.client.impl.Topic;
import io.zeebe.transport.*;


public class TopologyImpl implements Topology
{
    protected Map<Topic, RemoteAddress> topicLeaders;
    protected List<RemoteAddress> brokers;
    protected final Random randomBroker = new Random();

    public TopologyImpl()
    {
        topicLeaders = new HashMap<>();
        brokers = new ArrayList<>();
    }

    public void addBroker(RemoteAddress remoteAddress)
    {
        brokers.add(remoteAddress);
    }

    @Override
    public RemoteAddress getLeaderForTopic(Topic topic)
    {
        if (topic != null)
        {
            return topicLeaders.get(topic);
        }
        else
        {
            return getRandomBroker();
        }
    }

    @Override
    public RemoteAddress getRandomBroker()
    {
        if (!brokers.isEmpty())
        {
            final int nextBroker = randomBroker.nextInt(brokers.size());
            return brokers.get(nextBroker);
        }
        else
        {
            throw new RuntimeException("Unable to select random broker from empty list");
        }
    }

    @Override
    public String toString()
    {
        return "Topology{" +
            "topicLeaders=" + topicLeaders +
            ", brokers=" + brokers +
            '}';
    }

    public void update(TopologyResponse topologyDto, ClientTransport transport)
    {
        for (SocketAddress addr : topologyDto.getBrokers())
        {
            addBroker(transport.registerRemoteAddress(addr));
        }

        for (TopicLeader leader : topologyDto.getTopicLeaders())
        {
            topicLeaders.put(leader.getTopic(), transport.registerRemoteAddress(leader.getSocketAddress()));
        }
    }

}
