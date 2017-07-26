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
package io.zeebe.client.impl.topology;

import java.util.*;

import io.zeebe.client.impl.Partition;
import io.zeebe.client.topology.Topology;
import io.zeebe.transport.*;


public class TopologyImpl implements Topology
{
    protected Map<Partition, RemoteAddress> partitionLeaders;
    protected List<RemoteAddress> brokers;
    protected final Random randomBroker = new Random();

    public TopologyImpl()
    {
        partitionLeaders = new HashMap<>();
        brokers = new ArrayList<>();
    }

    public void addBroker(RemoteAddress remoteAddress)
    {
        brokers.add(remoteAddress);
    }

    @Override
    public RemoteAddress getLeaderForPartition(Partition partition)
    {
        if (partition != null)
        {
            return partitionLeaders.get(partition);
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
            "topicLeaders=" + partitionLeaders +
            ", brokers=" + brokers +
            '}';
    }

    public void update(TopologyResponse topologyDto, ClientTransport transport)
    {
        for (SocketAddress addr : topologyDto.getBrokers())
        {
            addBroker(transport.registerRemoteAddress(addr));
        }

        for (PartitionLeader leader : topologyDto.getTopicLeaders())
        {
            partitionLeaders.put(leader.getPartition(), transport.registerRemoteAddress(leader.getSocketAddress()));
        }
    }

}
