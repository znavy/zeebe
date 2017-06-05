package org.camunda.tngp.broker.clustering.management.topics;

import static org.camunda.tngp.broker.clustering.ClusterServiceNames.topicsManagementStreamProcessorName;
import static org.camunda.tngp.broker.logstreams.LogStreamServiceNames.SNAPSHOT_STORAGE_SERVICE;
import static org.camunda.tngp.broker.logstreams.processor.StreamProcessorIds.TOPIC_MANAGEMENT_PROCESSOR_ID;
import static org.camunda.tngp.broker.system.SystemServiceNames.AGENT_RUNNER_SERVICE;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.broker.clustering.management.ClusterManager;
import org.camunda.tngp.broker.logstreams.cfg.StreamProcessorCfg;
import org.camunda.tngp.broker.logstreams.processor.StreamProcessorService;
import org.camunda.tngp.broker.system.ConfigurationManager;
import org.camunda.tngp.broker.transport.clientapi.CommandResponseWriter;
import org.camunda.tngp.dispatcher.Dispatcher;
import org.camunda.tngp.hashindex.store.FileChannelIndexStore;
import org.camunda.tngp.logstreams.log.LogStream;
import org.camunda.tngp.logstreams.processor.StreamProcessorController;
import org.camunda.tngp.servicecontainer.Injector;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceGroupReference;
import org.camunda.tngp.servicecontainer.ServiceName;
import org.camunda.tngp.servicecontainer.ServiceStartContext;
import org.camunda.tngp.servicecontainer.ServiceStopContext;
import org.camunda.tngp.util.FileUtil;

/**
 * This class is responsible for starting the stream processing
 * for the
 *
 * Tracks all log streams which are installed and if the name is
 * {@link SystemTopics#ZB_TOPICS_MANAGEMENT}, it installs the
 * corresponding stream processors.
 *
 */
public class TopicsManagementStreamDecorator implements Service<TopicsManagementStreamDecorator>
{
    private static final DirectBuffer ZB_TOPICS_MANAGEMENT_NAME = new UnsafeBuffer(SystemTopics.ZB_TOPICS_MANAGEMENT.getName().getBytes(StandardCharsets.UTF_8));

    private final ServiceGroupReference<LogStream> logStreamsRef = ServiceGroupReference.<LogStream>create()
        .onAdd(this::onStreamOpened)
        .build();

    private final Injector<Dispatcher> sendBufferInjector = new Injector<>();

    private final Injector<ClusterManager> clusterManagerInjector = new Injector<>();

    private final StreamProcessorCfg streamProcessorCfg;

    private FileChannelIndexStore indexStore;

    private ServiceStartContext serviceStartContext;

    public TopicsManagementStreamDecorator(final ConfigurationManager configurationManager)
    {
        streamProcessorCfg = configurationManager.readEntry("index", StreamProcessorCfg.class);
    }

    @Override
    public TopicsManagementStreamDecorator get()
    {
        return this;
    }

    @Override
    public void start(ServiceStartContext ctx)
    {
        this.serviceStartContext = ctx;

        ctx.run(() ->
        {
            final String indexDirectory = streamProcessorCfg.directory;

            if (indexDirectory != null && !indexDirectory.isEmpty())
            {
                final String indexFile = indexDirectory + File.separator + "default.idx";
                final FileChannel indexFileChannel = FileUtil.openChannel(indexFile, true);

                indexStore = new FileChannelIndexStore(indexFileChannel);
            }
            else
            {
                throw new RuntimeException("Cannot create topics management stream processor index, no index file name provided.");
            }
        });
    }

    @Override
    public void stop(ServiceStopContext ctx)
    {
        ctx.run(() ->
        {
            indexStore.flush();
            indexStore.close();
        });
    }

    private void onStreamOpened(ServiceName<LogStream> logStreamServiceName, LogStream service)
    {
        if (!ZB_TOPICS_MANAGEMENT_NAME.equals(service.getTopicName()))
        {
            return;
        }

        final String logName = service.getLogName();

        final ServiceName<StreamProcessorController> streamProcessorServiceName = topicsManagementStreamProcessorName(logName);
        final String streamProcessorName = streamProcessorServiceName.getName();


        final Dispatcher sendBuffer = sendBufferInjector.getValue();
        final CommandResponseWriter responseWriter = new CommandResponseWriter(sendBuffer);

        final TopicsManagementStreamProcessor streamProcessor = new TopicsManagementStreamProcessor(responseWriter, indexStore, clusterManagerInjector.getValue());
        final StreamProcessorService streamProcessorService = new StreamProcessorService(
                streamProcessorName,
                TOPIC_MANAGEMENT_PROCESSOR_ID,
                streamProcessor)
                .eventFilter(TopicsManagementStreamProcessor.eventFilter());

        serviceStartContext.createService(streamProcessorServiceName, streamProcessorService)
              .dependency(logStreamServiceName, streamProcessorService.getSourceStreamInjector())
              .dependency(logStreamServiceName, streamProcessorService.getTargetStreamInjector())
              .dependency(SNAPSHOT_STORAGE_SERVICE, streamProcessorService.getSnapshotStorageInjector())
              .dependency(AGENT_RUNNER_SERVICE, streamProcessorService.getAgentRunnerInjector())
              .install();
    }

    public ServiceGroupReference<LogStream> getLogStreamsRef()
    {
        return logStreamsRef;
    }

    public Injector<Dispatcher> getSendBufferInjector()
    {
        return sendBufferInjector;
    }

    public Injector<ClusterManager> getClusterManagerInjector()
    {
        return clusterManagerInjector;
    }
}
