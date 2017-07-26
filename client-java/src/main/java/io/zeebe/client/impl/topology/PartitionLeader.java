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

import io.zeebe.client.impl.Partition;
import io.zeebe.transport.SocketAddress;


public class PartitionLeader
{
    protected String host;
    protected int port;
    protected String topicName;
    protected int partitionId;

    public PartitionLeader setHost(final String host)
    {
        this.host = host;
        return this;
    }

    public PartitionLeader setPort(final int port)
    {
        this.port = port;
        return this;
    }

    public PartitionLeader setTopicName(final String topicName)
    {
        this.topicName = topicName;
        return this;
    }

    public PartitionLeader setPartitionId(final int partitionId)
    {
        this.partitionId = partitionId;
        return this;
    }

    public Partition getPartition()
    {
        return new Partition(topicName, partitionId);
    }


    public SocketAddress getSocketAddress()
    {
        return new SocketAddress(host, port);
    }

}
