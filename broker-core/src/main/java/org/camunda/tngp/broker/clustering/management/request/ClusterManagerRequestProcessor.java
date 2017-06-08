package org.camunda.tngp.broker.clustering.management.request;

import org.agrona.DirectBuffer;
import org.camunda.tngp.broker.clustering.management.ClusterManager;
import org.camunda.tngp.broker.clustering.management.message.PartitionManagementRequest;
import org.camunda.tngp.broker.clustering.management.message.PartitionManagementResponse;
import org.camunda.tngp.broker.clustering.util.AsyncRequestProcessor;
import org.camunda.tngp.broker.clustering.util.MessageWriter;
import org.camunda.tngp.broker.clustering.util.RequestData;
import org.camunda.tngp.broker.clustering.util.RequestHandler;
import org.camunda.tngp.clustering.management.MessageHeaderDecoder;
import org.camunda.tngp.clustering.management.PartitionManagementRequestDecoder;

public class ClusterManagerRequestProcessor implements AsyncRequestProcessor
{
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();

    private final PartitionManagementRequest request = new PartitionManagementRequest();
    private final PartitionManagementResponse response = new PartitionManagementResponse();

    private final ClusterManager clusterManager;

    public ClusterManagerRequestProcessor(final ClusterManager clusterManager)
    {
        this.clusterManager = clusterManager;
    }

    private final BoostrapPartitionHandler bootstrapPartitionHandler = new BoostrapPartitionHandler();
    private final InviteHandler inviteHandler = new InviteHandler();

    @Override
    public RequestHandler selectHandler(RequestData requestData)
    {
        final DirectBuffer msgBuffer = requestData.getMsgBuffer();

        messageHeaderDecoder.wrap(msgBuffer, 0);

        final int schemaId = messageHeaderDecoder.schemaId();
        final int templateId = messageHeaderDecoder.templateId();

        RequestHandler handler = null;

        if (PartitionManagementRequestDecoder.SCHEMA_ID == schemaId
                && PartitionManagementRequestDecoder.TEMPLATE_ID == templateId)
        {
            request.wrap(msgBuffer, 0, requestData.getMsgLength());

            switch (request.opCode())
            {
                case BOOTSTRAP:
                    handler = bootstrapPartitionHandler;
                    break;

                case INVITE:
                    handler = inviteHandler;
                    break;

                default:
                    break;
            }
        }

        return handler;

    }

    private class BoostrapPartitionHandler implements RequestHandler
    {
        @Override
        public void handleRequest()
        {

        }

        @Override
        public boolean sendRespone(MessageWriter messageWriter)
        {
            response.reset();

            return messageWriter
                .message(response)
                .tryWriteMessage();
        }
    }

    private class InviteHandler implements RequestHandler
    {
        @Override
        public void handleRequest()
        {

        }

        @Override
        public boolean sendRespone(MessageWriter messageWriter)
        {
            response.reset();

            return messageWriter
                .message(response)
                .tryWriteMessage();
        }
    }
}
