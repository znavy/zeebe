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
package io.zeebe.client.impl.cmd;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.client.*;
import io.zeebe.client.event.Event;
import io.zeebe.client.impl.Partition;
import io.zeebe.client.impl.topology.ClientTopologyManager;
import io.zeebe.protocol.clientapi.*;
import io.zeebe.transport.*;
import io.zeebe.util.buffer.BufferReader;
import io.zeebe.util.buffer.BufferUtil;
import io.zeebe.util.buffer.BufferWriter;
import io.zeebe.util.state.*;
import org.agrona.*;
import org.agrona.io.DirectBufferInputStream;

@SuppressWarnings("rawtypes")
public class ClientCommandController implements BufferReader, BufferWriter
{
    private static final long CMD_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

    protected static final int TRANSITION_DEFAULT = 0;
    protected static final int TRANSITION_FAILED = 1;
    protected static final int TRANSITION_REFRESH_TOPOLOGY = 2;

    protected final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    protected final ErrorResponseDecoder errorResponseDecoder = new ErrorResponseDecoder();
    protected final ExecuteCommandResponseDecoder commandResponseDecoder = new ExecuteCommandResponseDecoder();

    protected final StateMachine<Context> stateMachine;
    protected final DetermineRemoteState determineRemoteState = new DetermineRemoteState();
    protected final RefreshTopologyState refreshTopologyState = new RefreshTopologyState();
    protected final AwaitTopologyRefreshState awaitTopologyRefreshState = new AwaitTopologyRefreshState();
    protected final ExecuteRequestState executeRequestState = new ExecuteRequestState();
    protected final HandleResponseState handleResponseState = new HandleResponseState();
    protected final FinishedState finishedState = new FinishedState();
    protected final FailedState failedState = new FailedState();
    protected final ClosedState closedState = new ClosedState();

    protected final ClientTopologyManager topologyManager;
    protected final CommandRequestWriter commandWriter = null;
    protected final ObjectMapper objectMapper = null;

    protected Event event;
    private String expectedState;
    protected CompletableFuture future;

    protected final ClientTransport transport;

    protected final ExpandableArrayBuffer commandBuffer = new ExpandableArrayBuffer();
    protected int commandLength = 0;

    protected boolean isConfigured = false;

    private Consumer<ClientCommandController> closeConsumer;


    public ClientCommandController(final ClientTransport transport, final ClientTopologyManager topologyManager, Consumer<ClientCommandController> closeConsumer)
    {
        this.transport = transport;
        this.topologyManager = topologyManager;
        this.closeConsumer = closeConsumer;

        stateMachine = StateMachine.<Context>builder(Context::new)
            .initialState(closedState)
            .from(closedState).take(TRANSITION_DEFAULT).to(determineRemoteState)
            .from(determineRemoteState).take(TRANSITION_DEFAULT).to(executeRequestState)
            .from(determineRemoteState).take(TRANSITION_REFRESH_TOPOLOGY).to(refreshTopologyState)
            .from(determineRemoteState).take(TRANSITION_FAILED).to(failedState)
            .from(refreshTopologyState).take(TRANSITION_DEFAULT).to(awaitTopologyRefreshState)
            .from(awaitTopologyRefreshState).take(TRANSITION_DEFAULT).to(determineRemoteState)
            .from(awaitTopologyRefreshState).take(TRANSITION_FAILED).to(determineRemoteState)
            .from(executeRequestState).take(TRANSITION_DEFAULT).to(handleResponseState)
            .from(executeRequestState).take(TRANSITION_REFRESH_TOPOLOGY).to(refreshTopologyState)
            .from(executeRequestState).take(TRANSITION_FAILED).to(failedState)
            .from(handleResponseState).take(TRANSITION_DEFAULT).to(finishedState)
            .from(handleResponseState).take(TRANSITION_FAILED).to(failedState)
            .from(handleResponseState).take(TRANSITION_REFRESH_TOPOLOGY).to(refreshTopologyState)
            .from(finishedState).take(TRANSITION_DEFAULT).to(closedState)
            .from(failedState).take(TRANSITION_DEFAULT).to(closedState)
            .build();
    }


    public void configure(Event event, String expectedState, CompletableFuture<Event> future)
    {
        this.event = event;
        this.expectedState = expectedState;
        this.future = future;
        this.commandWriter.write(event, commandBuffer);
        this.isConfigured = true;
    }

    public int doWork()
    {
        return stateMachine.doWork();
    }

    public boolean isClosed()
    {
        return stateMachine.getCurrentState() == closedState && !isConfigured;
    }

    @Override
    public int getLength()
    {
        return commandLength;
    }

    @Override
    public void write(MutableDirectBuffer buffer, int offset)
    {
        buffer.putBytes(offset, commandBuffer, 0, commandLength);
    }

    @Override
    public void wrap(final DirectBuffer buffer, final int offset, final int length)
    {
        messageHeaderDecoder.wrap(buffer, offset);

        final int schemaId = messageHeaderDecoder.schemaId();
        final int templateId = messageHeaderDecoder.templateId();
        final int blockLength = messageHeaderDecoder.blockLength();
        final int version = messageHeaderDecoder.version();

        final int responseMessageOffset = offset + messageHeaderDecoder.encodedLength();

        final Context context = stateMachine.getContext();

        commandResponseDecoder.wrap(buffer, responseMessageOffset, blockLength, version);

        if (schemaId == commandResponseDecoder.sbeSchemaId() && templateId == commandResponseDecoder.sbeTemplateId())
        {
            final TopicMetadata topicMetadata = new TopicMetadata();
            topicMetadata.setKey(commandResponseDecoder.key());
            topicMetadata.setEventType(event.getTopicMetadata().getEventType());
            topicMetadata.setPartitionId(commandResponseDecoder.partitionId());
            topicMetadata.setTopicName(commandResponseDecoder.topicName());

            final int eventLength = commandResponseDecoder.eventLength();

            final DirectBufferInputStream eventInputStream = new DirectBufferInputStream(buffer,  commandResponseDecoder.limit(), eventLength);

            Event resultEvent = null;
            try
            {
                resultEvent = objectMapper.readValue(eventInputStream, event.getClass());
            }
            catch (Exception e)
            {
                throw new RuntimeException("Unexpected execption while deserializing command", e);
            }

            if (!expectedState.equals(resultEvent.getEventType()))
            {
                throw new FailedCommandException(event, resultEvent);
            }


            // expose request channel if need to keep a reference of it (e.g. subscriptions)
//            if (responseObject instanceof ReceiverAwareResponseResult)
//            {
//                ((ReceiverAwareResponseResult) responseObject).setReceiver(context.receiveRemote);
//            }

            context.responseObject = resultEvent;
        }
        else
        {
            errorResponseDecoder.wrap(buffer, responseMessageOffset, blockLength, version);

            final int errorDataLength = errorResponseDecoder.errorDataLength();
            context.errorBuffer = BufferUtil.wrapArray(new byte[errorDataLength]);
            context.errorCode = errorResponseDecoder.errorCode();

            errorResponseDecoder.getErrorData(context.errorBuffer, 0, errorDataLength);
        }

    }

    private class DetermineRemoteState implements State<Context>
    {
        @Override
        public int doWork(final Context context) throws Exception
        {
            final long now = System.currentTimeMillis();
            final TopicMetadata topicMetadata = event.getTopicMetadata();
            final Partition partition = new Partition(topicMetadata.getTopicName(), topicMetadata.getPartitionId());

            if (now > context.timeout)
            {
                context.exception = new RuntimeException("Cannot execute command. No broker for topic with name '" + partition.getTopicName() + "' and partition id '" + partition.getPartitionId() + "' found.", context.exception);
                context.take(TRANSITION_FAILED);
                return 0;
            }

            context.receiveRemote = topologyManager.getLeaderForTopic(partition);

            if (context.receiveRemote != null)
            {
                final ClientRequest request = transport.getOutput().sendRequest(context.receiveRemote, ClientCommandController.this);

                if (request != null)
                {
                    context.request = request;
                    context.take(TRANSITION_DEFAULT);
                }
            }
            else
            {
                context.take(TRANSITION_REFRESH_TOPOLOGY);
            }

            return 1;
        }

    }
    private class RefreshTopologyState implements State<Context>
    {

        @Override
        public int doWork(final Context context) throws Exception
        {
            context.topologyRefreshFuture = topologyManager.refreshNow();
            context.take(TRANSITION_DEFAULT);
            return 1;
        }

    }
    private class AwaitTopologyRefreshState implements State<Context>
    {
        @Override
        public int doWork(final Context context) throws Exception
        {
            int workCount = 0;

            final CompletableFuture<Void> topologyRefreshFuture = context.topologyRefreshFuture;

            if (topologyRefreshFuture.isDone())
            {
                try
                {
                    topologyRefreshFuture.get();
                    context.take(TRANSITION_DEFAULT);
                }
                catch (final Exception e)
                {
                    context.exception = e;
                    context.take(TRANSITION_FAILED);
                }


                workCount += 1;
            }

            return workCount;
        }

    }

    private class ExecuteRequestState implements State<Context>
    {
        @Override
        public int doWork(final Context context) throws Exception
        {
            final ClientRequest request = context.request;

            if (request.isDone())
            {
                try
                {
                    final DirectBuffer response = request.get();
                    wrap(response, 0, response.capacity());

                    context.take(TRANSITION_DEFAULT);
                }
                catch (ClientCommandRejectedException e)
                {
                    context.exception = e;
                    context.take(TRANSITION_FAILED);
                }
                catch (Throwable e)
                {
                    context.take(TRANSITION_REFRESH_TOPOLOGY);
                }
                finally
                {
                    request.close();
                }

                return 1;
            }
            else
            {
                // wait
                return 0;
            }
        }
    }

    private class HandleResponseState implements State<Context>
    {

        @Override
        public int doWork(final Context context) throws Exception
        {
            final ErrorCode errorCode = context.errorCode;

            if (errorCode == ErrorCode.NULL_VAL)
            {
                // request was successful
                context.take(TRANSITION_DEFAULT);
            }
            else if (errorCode == ErrorCode.TOPIC_NOT_FOUND)
            {
                // reset error context
                context.errorCode = ErrorCode.NULL_VAL;
                context.errorBuffer = null;

                // topic not found -> refresh topology -> retry request
                context.take(TRANSITION_REFRESH_TOPOLOGY);
            }
            else
            {
                context.take(TRANSITION_FAILED);
            }

            return 1;
        }

    }

    private class FinishedState implements State<Context>
    {

        @Override
        public int doWork(final Context context) throws Exception
        {
            future.complete(context.responseObject);
            context.take(TRANSITION_DEFAULT);
            return 1;
        }

        @Override
        public void onExit()
        {
            closeConsumer.accept(ClientCommandController.this);
        }
    }

    private class FailedState implements State<Context>
    {

        @Override
        public int doWork(final Context context) throws Exception
        {
            final ErrorCode errorCode = context.errorCode;
            Throwable exception = context.exception;

            if (errorCode != ErrorCode.NULL_VAL)
            {
                try
                {
                    final String errorMessage = BufferUtil.bufferAsString(context.errorBuffer);
                    exception = new BrokerRequestException(context.errorCode, errorMessage);
                }
                catch (final Exception e)
                {
                    exception = new BrokerRequestException(errorCode, "Unable to parse error message from response: " + e.getMessage());
                }
            }
            else if (exception == null)
            {
                exception = new RuntimeException("Unknown error during request execution");
            }

            future.completeExceptionally(exception);

            context.take(TRANSITION_DEFAULT);

            return 1;
        }

        @Override
        public void onExit()
        {
            closeConsumer.accept(ClientCommandController.this);
        }
    }

    private class ClosedState implements WaitState<Context>
    {


        @Override
        public void work(final Context context) throws Exception
        {
            if (isConfigured)
            {
                context.reset();
                context.timeout = System.currentTimeMillis() + CMD_TIMEOUT;
                isConfigured = false;
                context.take(TRANSITION_DEFAULT);
            }
        }
    }

    static class Context extends SimpleStateMachineContext
    {
        public RemoteAddress receiveRemote;

        public ClientRequest request;

        CompletableFuture<Void> topologyRefreshFuture;

        long timeout;
        Object responseObject;
        ErrorCode errorCode = ErrorCode.NULL_VAL;
        MutableDirectBuffer errorBuffer;
        Throwable exception;

        Context(final StateMachine<?> stateMachine)
        {
            super(stateMachine);
        }

        @Override
        public void reset()
        {
            topologyRefreshFuture = null;
            responseObject = null;
            errorCode = ErrorCode.NULL_VAL;
            errorBuffer = null;
            exception = null;
        }

    }
}
