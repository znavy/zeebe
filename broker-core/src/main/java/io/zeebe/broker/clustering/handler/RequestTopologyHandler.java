/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.clustering.handler;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import io.zeebe.broker.clustering.management.MemberListService;
import io.zeebe.broker.clustering.management.MemberRaftComposite;
import io.zeebe.broker.transport.clientapi.ErrorResponseWriter;
import io.zeebe.broker.transport.controlmessage.ControlMessageHandler;
import io.zeebe.broker.transport.controlmessage.ControlMessageResponseWriter;
import io.zeebe.logstreams.log.LogStream;
import io.zeebe.msgpack.property.ArrayProperty;
import io.zeebe.msgpack.value.ValueArray;
import io.zeebe.protocol.clientapi.ControlMessageType;
import io.zeebe.protocol.clientapi.ErrorCode;
import io.zeebe.protocol.impl.BrokerEventMetadata;
import io.zeebe.raft.Raft;
import io.zeebe.raft.state.RaftState;
import io.zeebe.transport.ServerOutput;
import io.zeebe.transport.SocketAddress;
import org.agrona.DirectBuffer;

public class RequestTopologyHandler implements ControlMessageHandler
{

    protected final MemberListService memberListService;
    protected final ControlMessageResponseWriter responseWriter;
    protected final ErrorResponseWriter errorResponseWriter;

    public RequestTopologyHandler(final ServerOutput ouput, final MemberListService memberListService)
    {
        this.memberListService = memberListService;
        this.responseWriter = new ControlMessageResponseWriter(ouput);
        this.errorResponseWriter = new ErrorResponseWriter(ouput);
    }

    @Override
    public ControlMessageType getMessageType()
    {
        return ControlMessageType.REQUEST_TOPOLOGY;
    }

    @Override
    public CompletableFuture<Void> handle(int partitionId, final DirectBuffer buffer, final BrokerEventMetadata metadata)
    {

        final Iterator<MemberRaftComposite> iterator = memberListService.iterator();
        final Topology topology = new Topology();
        while (iterator.hasNext())
        {
            final MemberRaftComposite next = iterator.next();

            final ArrayProperty<BrokerAddress> brokers = topology.brokers();

            final SocketAddress clientApi = next.getClientApi();
            brokers.add()
                   .setHost(clientApi.getHostBuffer(), 0, clientApi.hostLength())
                   .setPort(clientApi.port());

            final ValueArray<TopicLeader> topicLeaders = topology.topicLeaders();
            final Iterator<Raft> raftIterator = next.getRaftIterator();
            while (raftIterator.hasNext())
            {
                final Raft nextRaft = raftIterator.next();

                if (nextRaft.getState() == RaftState.LEADER)
                {
                    final LogStream logStream = nextRaft.getLogStream();
                    topicLeaders.add()
                                .setHost(clientApi.getHostBuffer(), 0, clientApi.hostLength())
                                .setPort(clientApi.port())
                                .setTopicName(logStream.getTopicName(), 0, logStream.getTopicName()
                                                                                    .capacity())
                                .setPartitionId(logStream.getPartitionId());

                }
            }
        }

        responseWriter.dataWriter(topology);

        if (!responseWriter.tryWriteResponse(metadata.getRequestStreamId(), metadata.getRequestId()))
        {
            errorResponseWriter.errorCode(ErrorCode.REQUEST_WRITE_FAILURE)
                               .errorMessage("Cannot write topology response.")
                               .failedRequest(buffer, 0, buffer.capacity())
                               .tryWriteResponseOrLogFailure(metadata.getRequestStreamId(), metadata.getRequestId());
        }

        return null;

    }

}
