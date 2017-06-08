package org.camunda.tngp.broker.clustering.management.topics;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import org.camunda.tngp.broker.Constants;
import org.camunda.tngp.broker.clustering.management.ClusterManager;
import org.camunda.tngp.broker.logstreams.BrokerEventMetadata;
import org.camunda.tngp.broker.logstreams.processor.MetadataFilter;
import org.camunda.tngp.broker.transport.clientapi.CommandResponseWriter;
import org.camunda.tngp.hashindex.store.FileChannelIndexStore;
import org.camunda.tngp.logstreams.log.LogStream;
import org.camunda.tngp.logstreams.log.LogStreamWriter;
import org.camunda.tngp.logstreams.log.LoggedEvent;
import org.camunda.tngp.logstreams.processor.EventProcessor;
import org.camunda.tngp.logstreams.processor.StreamProcessor;
import org.camunda.tngp.logstreams.processor.StreamProcessorContext;
import org.camunda.tngp.logstreams.snapshot.SerializableWrapper;
import org.camunda.tngp.logstreams.spi.SnapshotSupport;
import org.camunda.tngp.protocol.clientapi.EventType;

public class TopicsManagementStreamProcessor implements StreamProcessor
{
    private final ClusterManager clusterManager;

    private final SerializableWrapper<HashSet<String>> topicNames;

    private final TopicManagementEvent evt = new TopicManagementEvent();

    private final BrokerEventMetadata sourceEventMetadata = new BrokerEventMetadata();
    private final BrokerEventMetadata targetEventMetadata = new BrokerEventMetadata();

    private final CommandResponseWriter responseWriter;

    private final CreateProcessor createProcessor = new CreateProcessor();
    private final CreatingProcessor creatingProcessor = new CreatingProcessor();
    private final CreatedProcessor createdProcessor = new CreatedProcessor();
    private final DeleteProcessor deleteProcessor = new DeleteProcessor();
    private final DeletingProcessor deletingProcessor = new DeletingProcessor();
    private final DeletedProcessor deletedProcessor = new DeletedProcessor();

    private long key;

    private LogStream targetStream;


    public TopicsManagementStreamProcessor(CommandResponseWriter responseWriter, FileChannelIndexStore indexStore, ClusterManager clusterManager)
    {
        this.clusterManager = clusterManager;
        this.responseWriter = responseWriter;
        this.topicNames = new SerializableWrapper<>(new HashSet<>());
    }

    @Override
    public void onOpen(StreamProcessorContext context)
    {
        targetStream = context.getTargetStream();
    }

    @Override
    public SnapshotSupport getStateResource()
    {
        return topicNames;
    }

    @Override
    public EventProcessor onEvent(LoggedEvent event)
    {
        key = event.getKey();

        event.readMetadata(sourceEventMetadata);
        event.readValue(evt);

        EventProcessor processor = null;

        switch (evt.getEventType())
        {
            case CREATE:
                processor = createProcessor;
                break;

            case CREATING:
                processor = creatingProcessor;
                break;

            case CREATED:
                processor = createdProcessor;
                break;

            case DELETE:
                processor = deleteProcessor;
                break;

            case DELETING:
                processor = deletingProcessor;
                break;

            case DELETED:
                processor = deletedProcessor;
                break;

            default:
                break;
        }

        return processor;
    }

    public static MetadataFilter eventFilter()
    {
        return (m) -> m.getEventType() == EventType.TOPIC_MANAGEMENT_EVENT;
    }

    private class CreateProcessor implements EventProcessor
    {
        @Override
        public void processEvent()
        {
            final boolean isRejected = topicNames.getObject().contains(evt.getTopicNameString());

            if (isRejected)
            {
                evt.setEventType(TopicManagementEventType.CREATE_REJECTED);
            }
            else
            {
                evt.setEventType(TopicManagementEventType.CREATING);
                evt.setPartitionIdx(0);
            }
        }

        @Override
        public long writeEvent(LogStreamWriter writer)
        {
            targetEventMetadata.reset();
            targetEventMetadata.eventType(EventType.TOPIC_MANAGEMENT_EVENT)
                .protocolVersion(Constants.PROTOCOL_VERSION)
                .raftTermId(targetStream.getTerm())
                .reqChannelId(sourceEventMetadata.getReqChannelId())
                .reqConnectionId(sourceEventMetadata.getReqConnectionId())
                .reqRequestId(sourceEventMetadata.getReqRequestId());

            return writer.key(key)
                .metadataWriter(targetEventMetadata)
                .valueWriter(evt)
                .tryWrite();
        }

        @Override
        public boolean executeSideEffects()
        {
            boolean result = true;

            if (evt.getEventType() == TopicManagementEventType.CREATE_REJECTED)
            {
                result = responseWriter
                    .topicName(targetStream.getTopicName())
                    .partitionId(targetStream.getPartitionId())
                    .brokerEventMetadata(sourceEventMetadata)
                    .eventWriter(evt)
                    .key(key)
                    .tryWriteResponse();
            }

            return result;
        }

        @Override
        public void updateState()
        {
            topicNames.getObject().add(evt.getTopicNameString());
        }
    }

    private class CreatingProcessor implements EventProcessor
    {
        CompletableFuture<Void> topicFuture;
        int partitionCreateIdx;

        @Override
        public void processEvent()
        {
            topicFuture = null;
            partitionCreateIdx = evt.getPartitionIdx();
        }

        @Override
        public boolean executeSideEffects()
        {
            boolean result = false;

            if (topicFuture == null)
            {
                topicFuture = clusterManager.createTopicPartition(evt.getTopicName(), partitionCreateIdx);
            }
            else
            {
                if (topicFuture.isDone())
                {
                    result = true;

                    try
                    {
                        topicFuture.get();

                        evt.setPartitionIdx(partitionCreateIdx + 1);

                        if (partitionCreateIdx == evt.getPartitionCount() - 1)
                        {
                            evt.setEventType(TopicManagementEventType.CREATED);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        evt.setEventType(TopicManagementEventType.FAILING);
                    }
                }
            }

            return result;
        }

        @Override
        public long writeEvent(LogStreamWriter writer)
        {
            targetEventMetadata.reset();
            targetEventMetadata.eventType(EventType.TOPIC_MANAGEMENT_EVENT)
                .protocolVersion(Constants.PROTOCOL_VERSION)
                .raftTermId(targetStream.getTerm())
                .reqChannelId(sourceEventMetadata.getReqChannelId())
                .reqConnectionId(sourceEventMetadata.getReqConnectionId())
                .reqRequestId(sourceEventMetadata.getReqRequestId());

            return writer.key(key)
                .metadataWriter(targetEventMetadata)
                .valueWriter(evt)
                .tryWrite();
        }
    }

    public class CreatedProcessor implements EventProcessor
    {
        @Override
        public void processEvent()
        {
            // ignore
        }

        @Override
        public boolean executeSideEffects()
        {
            return responseWriter
                .topicName(targetStream.getTopicName())
                .partitionId(targetStream.getPartitionId())
                .brokerEventMetadata(sourceEventMetadata)
                .eventWriter(evt)
                .key(key)
                .tryWriteResponse();
        }
    }

    public class DeleteProcessor implements EventProcessor
    {

        @Override
        public void processEvent()
        {
            final boolean isRejected = !topicNames.getObject().contains(evt.getTopicNameString());

            if (isRejected)
            {
                evt.setEventType(TopicManagementEventType.DELETE_REJECTED);
            }
            else
            {
                evt.setEventType(TopicManagementEventType.DELETING);
                evt.setPartitionIdx(0);
            }
        }


        @Override
        public long writeEvent(LogStreamWriter writer)
        {
            targetEventMetadata.reset();
            targetEventMetadata.eventType(EventType.TOPIC_MANAGEMENT_EVENT)
                .protocolVersion(Constants.PROTOCOL_VERSION)
                .raftTermId(targetStream.getTerm())
                .reqChannelId(sourceEventMetadata.getReqChannelId())
                .reqConnectionId(sourceEventMetadata.getReqConnectionId())
                .reqRequestId(sourceEventMetadata.getReqRequestId());

            return writer.key(key)
                .metadataWriter(targetEventMetadata)
                .valueWriter(evt)
                .tryWrite();
        }

        @Override
        public boolean executeSideEffects()
        {
            boolean result = true;

            if (evt.getEventType() == TopicManagementEventType.DELETE_REJECTED)
            {
                result = responseWriter
                    .topicName(targetStream.getTopicName())
                    .partitionId(targetStream.getPartitionId())
                    .brokerEventMetadata(sourceEventMetadata)
                    .eventWriter(evt)
                    .key(key)
                    .tryWriteResponse();
            }

            return result;
        }
    }

    public class DeletingProcessor implements EventProcessor
    {
        CompletableFuture<Void> topicFuture;
        int partitionRemoveIdx;

        @Override
        public void processEvent()
        {
            topicFuture = null;
            partitionRemoveIdx = evt.getPartitionIdx();
        }

        @Override
        public boolean executeSideEffects()
        {
            boolean result = false;

            if (topicFuture == null)
            {
                topicFuture = clusterManager.removeTopicPartition(evt.getTopicName(), partitionRemoveIdx);
            }
            else
            {
                if (topicFuture.isDone())
                {
                    result = true;

                    try
                    {
                        topicFuture.get();
                        evt.setPartitionIdx(partitionRemoveIdx + 1);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    if (evt.getPartitionIdx() == evt.getPartitionCount())
                    {
                        evt.setEventType(TopicManagementEventType.DELETED);
                    }
                }
            }

            return result;
        }

        @Override
        public long writeEvent(LogStreamWriter writer)
        {
            targetEventMetadata.reset();
            targetEventMetadata.eventType(EventType.TOPIC_MANAGEMENT_EVENT)
                .protocolVersion(Constants.PROTOCOL_VERSION)
                .raftTermId(targetStream.getTerm())
                .reqChannelId(sourceEventMetadata.getReqChannelId())
                .reqConnectionId(sourceEventMetadata.getReqConnectionId())
                .reqRequestId(sourceEventMetadata.getReqRequestId());

            return writer.key(key)
                .metadataWriter(targetEventMetadata)
                .valueWriter(evt)
                .tryWrite();
        }

    }

    public class DeletedProcessor implements EventProcessor
    {
        @Override
        public void processEvent()
        {
            // ignore
        }

        @Override
        public boolean executeSideEffects()
        {
            return responseWriter
                .topicName(targetStream.getTopicName())
                .partitionId(targetStream.getPartitionId())
                .brokerEventMetadata(sourceEventMetadata)
                .eventWriter(evt)
                .key(key)
                .tryWriteResponse();
        }
    }
}
