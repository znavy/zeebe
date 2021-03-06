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
package io.zeebe.broker.system.log;

import io.zeebe.broker.transport.clientapi.ErrorResponseWriter;
import io.zeebe.broker.transport.controlmessage.ControlMessageHandler;
import io.zeebe.protocol.Protocol;
import io.zeebe.protocol.clientapi.ControlMessageType;
import io.zeebe.protocol.clientapi.ErrorCode;
import io.zeebe.protocol.impl.BrokerEventMetadata;
import io.zeebe.transport.ServerOutput;
import io.zeebe.util.sched.ActorControl;
import io.zeebe.util.sched.future.ActorFuture;
import org.agrona.DirectBuffer;

public class RequestPartitionsMessageHandler implements ControlMessageHandler
{
    protected final SystemPartitionManager systemPartitionManager;
    protected final ErrorResponseWriter errorWriter;

    public RequestPartitionsMessageHandler(ServerOutput output, SystemPartitionManager systemPartitionManager)
    {
        this.systemPartitionManager = systemPartitionManager;
        this.errorWriter = new ErrorResponseWriter(output);
    }

    @Override
    public ControlMessageType getMessageType()
    {
        return ControlMessageType.REQUEST_PARTITIONS;
    }

    @Override
    public void handle(ActorControl actor, int partitionId, DirectBuffer buffer, BrokerEventMetadata metadata)
    {
        final int requestStreamId = metadata.getRequestStreamId();
        final long requestId = metadata.getRequestId();

        if (partitionId != Protocol.SYSTEM_PARTITION)
        {
            sendErrorResponse(actor, ErrorCode.REQUEST_PROCESSING_FAILURE,
                "Partitions request must address the system partition " + Protocol.SYSTEM_PARTITION,
                requestStreamId, requestId);
        }
        else
        {
            // stream processor actor sends the response on success
            final ActorFuture<Void> handlerFuture = systemPartitionManager.sendPartitions(requestStreamId, requestId);

            actor.runOnCompletion(handlerFuture, ((aVoid, throwable) ->
            {
                if (throwable != null)
                {
                    // it is important that partition not found is returned here to signal a client that it may have addressed a broker
                    // that appeared as the system partition leader but is not (yet) able to respond
                    sendErrorResponse(actor, ErrorCode.PARTITION_NOT_FOUND,
                        throwable.getMessage(),
                        requestStreamId, requestId);
                }
            }));
        }
    }

    protected void sendErrorResponse(ActorControl actor, ErrorCode errorCode, String message, int requestStream, long requestId)
    {
        // Backpressure:
        // ControlMessageHandlerManager does not poll new messages, since
        // he is blocked, with the runUntileDone call, until the response is send successfully
        actor.runUntilDone(() ->
        {
            final boolean success = errorWriter.errorCode(errorCode)
                .errorMessage(message)
                .tryWriteResponse(requestStream, requestId);

            if (success)
            {
                actor.done();
            }
            else
            {
                actor.yield();
            }
        });
    }

}
