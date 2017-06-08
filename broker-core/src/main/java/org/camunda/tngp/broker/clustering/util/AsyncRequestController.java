package org.camunda.tngp.broker.clustering.util;

import org.camunda.tngp.dispatcher.Dispatcher;
import org.camunda.tngp.dispatcher.Subscription;
import org.camunda.tngp.transport.protocol.Protocols;
import org.camunda.tngp.util.state.SimpleStateMachineContext;
import org.camunda.tngp.util.state.State;
import org.camunda.tngp.util.state.StateMachine;
import org.camunda.tngp.util.state.StateMachineAgent;

/**
 * State machine controller for non-blocking, async request response interactions on the server-side.
 *
 * Delegates to an {@link AsyncRequestProcessor} for processing the requests.
 *
 */
public class AsyncRequestController
{
    private static final int TRANSITION_PROCESS = 1;
    private static final int TRANSITION_PROCESS_FAILED = 2;
    private static final int TRANSITION_AWAIT_ASYNC_COMPLETION = 3;
    private static final int TRANSITION_ASYNC_WORK_COMPLETED = 4;
    private static final int TRANSITION_RESPONSE_SENT = 5;
    private static final int TRANSITION_REQUEST_SKIPPED = 6;

    private final PollState pollState = new PollState();
    private final ProcessState processSate = new ProcessState();
    private final AwaitAsyncCompletionState awaitAsyncCompletionState = new AwaitAsyncCompletionState();
    private final SendResponseState sendResponseState = new SendResponseState();

    private final StateMachine<SimpleStateMachineContext> stateMachine = StateMachine.<SimpleStateMachineContext>builder(s -> new SimpleStateMachineContext(s))
        .from(pollState).take(TRANSITION_PROCESS).to(processSate)
        .from(processSate).take(TRANSITION_PROCESS_FAILED).to(pollState)
        .from(processSate).take(TRANSITION_REQUEST_SKIPPED).to(pollState)
        .from(processSate).take(TRANSITION_AWAIT_ASYNC_COMPLETION).to(awaitAsyncCompletionState)
        .from(awaitAsyncCompletionState).take(TRANSITION_ASYNC_WORK_COMPLETED).to(sendResponseState)
        .from(sendResponseState).take(TRANSITION_RESPONSE_SENT).to(pollState)
        .build();

    private final StateMachineAgent<SimpleStateMachineContext> stateMachineAgent = new StateMachineAgent<>(stateMachine);

    private final RequestData requestData = new RequestData();

    private final Subscription subscription;
    private final MessageWriter messageWriter;
    private final AsyncRequestProcessor requestProcessor;

    private RequestHandler handler;

    public AsyncRequestController(Subscription subscription, Dispatcher sendBuffer, AsyncRequestProcessor requestProcessor)
    {
        this.subscription = subscription;
        this.requestProcessor = requestProcessor;
        this.messageWriter = new MessageWriter(sendBuffer);
    }

    public StateMachineAgent<SimpleStateMachineContext> getStateMachineAgent()
    {
        return stateMachineAgent;
    }

    private class PollState implements State<SimpleStateMachineContext>
    {
        @Override
        public int doWork(SimpleStateMachineContext context) throws Exception
        {
            requestData.reset();

            final int workCount = subscription.poll(requestData, 1);

            if (requestData.getMsgLength() > 0)
            {
                context.take(TRANSITION_PROCESS);
            }

            return workCount;
        }
    }

    private class ProcessState implements State<SimpleStateMachineContext>
    {

        @Override
        public int doWork(SimpleStateMachineContext context) throws Exception
        {
            try
            {
                handler = requestProcessor.selectHandler(requestData);

                if (handler != null)
                {
                    handler.handleRequest();
                    context.take(TRANSITION_AWAIT_ASYNC_COMPLETION);
                }
                else
                {
                    context.take(TRANSITION_REQUEST_SKIPPED);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                // TODO: send error?
                context.take(TRANSITION_PROCESS_FAILED);
            }

            return 1;
        }

    }

    private class AwaitAsyncCompletionState implements State<SimpleStateMachineContext>
    {

        @Override
        public int doWork(SimpleStateMachineContext context) throws Exception
        {
            int workCount = 0;

            if (handler.isAsyncWorkComplete())
            {
                context.take(TRANSITION_ASYNC_WORK_COMPLETED);
                ++workCount;
            }
            else
            {
                // TODO: timeout
            }

            return workCount;
        }
    }

    private class SendResponseState implements State<SimpleStateMachineContext>
    {
        @Override
        public int doWork(SimpleStateMachineContext context) throws Exception
        {
            messageWriter.protocol(Protocols.REQUEST_RESPONSE)
                .channelId(requestData.getChannelId())
                .connectionId(requestData.getConnectionId())
                .requestId(requestData.getRequestId());

            if (handler.sendRespone(messageWriter))
            {
                context.take(TRANSITION_RESPONSE_SENT);
            }

            return 1;
        }
    }
}
