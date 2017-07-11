package io.zeebe.client.clustering;

import io.zeebe.client.impl.Topic;
import io.zeebe.transport.RemoteAddress;

public interface Topology
{
    RemoteAddress getLeaderForTopic(Topic topic);

    RemoteAddress getRandomBroker();
}
