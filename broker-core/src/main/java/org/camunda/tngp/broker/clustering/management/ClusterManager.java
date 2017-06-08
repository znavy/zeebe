package org.camunda.tngp.broker.clustering.management;

import static org.camunda.tngp.broker.clustering.ClusterServiceNames.*;
import static org.camunda.tngp.broker.system.SystemServiceNames.*;
import static org.camunda.tngp.broker.transport.TransportServiceNames.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.broker.clustering.channel.ClientChannelManagerService;
import org.camunda.tngp.broker.clustering.gossip.data.Peer;
import org.camunda.tngp.broker.clustering.management.config.ClusterManagementConfig;
import org.camunda.tngp.broker.clustering.management.message.PartitionManagementRequest;
import org.camunda.tngp.broker.clustering.management.message.PartitionManagementResponse;
import org.camunda.tngp.broker.clustering.management.request.ClusterManagerRequestProcessor;
import org.camunda.tngp.broker.clustering.management.topics.SystemTopics;
import org.camunda.tngp.broker.clustering.management.topics.TopicDefinition;
import org.camunda.tngp.broker.clustering.raft.Member;
import org.camunda.tngp.broker.clustering.raft.MetaStore;
import org.camunda.tngp.broker.clustering.raft.Raft;
import org.camunda.tngp.broker.clustering.raft.RaftContext;
import org.camunda.tngp.broker.clustering.raft.service.RaftContextService;
import org.camunda.tngp.broker.clustering.raft.service.RaftService;
import org.camunda.tngp.broker.clustering.service.SubscriptionService;
import org.camunda.tngp.broker.clustering.service.TransportConnectionPoolService;
import org.camunda.tngp.broker.clustering.util.AsyncRequestController;
import org.camunda.tngp.broker.clustering.util.MessageWriter;
import org.camunda.tngp.broker.clustering.util.RequestResponseController;
import org.camunda.tngp.broker.logstreams.LogStreamsFactory;
import org.camunda.tngp.dispatcher.FragmentHandler;
import org.camunda.tngp.dispatcher.Subscription;
import org.camunda.tngp.logstreams.impl.log.fs.FsLogStorage;
import org.camunda.tngp.logstreams.log.LogStream;
import org.camunda.tngp.servicecontainer.ServiceContainer;
import org.camunda.tngp.servicecontainer.ServiceName;
import org.camunda.tngp.transport.ChannelManager;
import org.camunda.tngp.transport.protocol.Protocols;
import org.camunda.tngp.transport.requestresponse.client.TransportConnectionPool;

public class ClusterManager implements Agent
{
    private final ClusterManagerContext context;
    private final ServiceContainer serviceContainer;

    private final List<Raft> rafts;

    private final ManyToOneConcurrentArrayQueue<Runnable> managementCmdQueue;
    private final Consumer<Runnable> commandConsumer;

    private final List<RequestResponseController> activeRequestController;

    private final AsyncRequestController requestController;

    private final PartitionManagementRequest partitionManagementRequest;
    private final PartitionManagementResponse partitionManagementResponse;

    private ClusterManagementConfig config;

    private final MessageWriter messageWriter;

    public ClusterManager(final ClusterManagerContext context, final ServiceContainer serviceContainer, ClusterManagementConfig config)
    {
        this.context = context;
        this.serviceContainer = serviceContainer;
        this.config = config;
        this.rafts = new CopyOnWriteArrayList<>();
        this.managementCmdQueue = new ManyToOneConcurrentArrayQueue<>(100);
        this.commandConsumer = (r) -> r.run();
        this.activeRequestController = new CopyOnWriteArrayList<>();

        this.partitionManagementRequest = new PartitionManagementRequest();
        this.partitionManagementResponse = new PartitionManagementResponse();

        this.messageWriter = new MessageWriter(context.getSendBuffer());
        this.requestController = new AsyncRequestController(context.getSubscription(),
                context.getSendBuffer(),
                new ClusterManagerRequestProcessor(this));

        context.getPeers().registerListener((p) -> addPeer(p));
    }

    public void open()
    {
        initMetaDirectory();

        final File[] metaFiles = getMetaFiles();

        if (metaFiles != null && metaFiles.length > 0)
        {
            resumeRafts(metaFiles);
        }
        else if (context.getPeers().sizeVolatile() == 1)
        {
            bootstrapSystemTopics();
        }
    }

    private void initMetaDirectory()
    {
        String metaDirectory = config.directory;

        if (metaDirectory == null || metaDirectory.isEmpty())
        {
            try
            {
                final File tempDir = Files.createTempDirectory("tngp-meta-").toFile();
                metaDirectory = tempDir.getAbsolutePath();
            }
            catch (IOException e)
            {
                throw new RuntimeException("Could not create temp directory for meta data", e);
            }
        }

        config.directory = metaDirectory;
    }

    private File[] getMetaFiles()
    {
        final File dir = new File(config.directory);

        if (!dir.exists())
        {
            try
            {
                dir.getParentFile().mkdirs();
                Files.createDirectory(dir.toPath());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return dir.listFiles();
    }

    protected void resumeRafts(File[] metaFiles)
    {
        final LogStreamsFactory logStreamsFactory = context.getLogStreamsFactory();

        for (int i = 0; i < metaFiles.length; i++)
        {
            final File file = metaFiles[i];
            final MetaStore meta = new MetaStore(file.getAbsolutePath());

            final int partitionId = meta.loadPartitionId();
            final DirectBuffer topicName = meta.loadTopicName();
            final String directory = meta.loadLogDirectory();

            final LogStream logStream = logStreamsFactory.openLogStream(topicName, partitionId, directory);

            createRaft(logStream, meta, Collections.emptyList(), false);
        }
    }

    protected void bootstrapSystemTopics()
    {
        createTopic(SystemTopics.DEFAULT_TOPIC);
        createTopic(SystemTopics.ZB_TOPICS_MANAGEMENT);
    }

    private void createTopic(TopicDefinition topicDef)
    {
        final LogStreamsFactory logStreamsFactory = context.getLogStreamsFactory();
        final DirectBuffer name = new UnsafeBuffer(topicDef.getName().getBytes(StandardCharsets.UTF_8));

        for (int i = 0; i < topicDef.getPartitionCount(); i++)
        {
            final LogStream logStream = logStreamsFactory.createLogStream(name, i);
            createRaft(logStream, Collections.emptyList(), true);
        }
    }

    @Override
    public String roleName()
    {
        return "management";
    }

    @Override
    public int doWork() throws Exception
    {
        int workcount = 0;

        workcount += managementCmdQueue.drain(commandConsumer);
        workcount += requestController.getStateMachineAgent().doWork();

        int i = 0;
        while (i < activeRequestController.size())
        {
            final RequestResponseController requestController = activeRequestController.get(i);
            workcount += requestController.doWork();

            if (requestController.isFailed() || requestController.isResponseAvailable())
            {
                requestController.close();
            }

            if (requestController.isClosed())
            {
                activeRequestController.remove(i);
            }
            else
            {
                i++;
            }
        }

        return workcount;
    }

    public void addPeer(final Peer peer)
    {
        final Peer copy = new Peer();
        copy.wrap(peer);
        managementCmdQueue.add(() ->
        {

            for (int i = 0; i < rafts.size(); i++)
            {
                final Raft raft = rafts.get(i);
                if (raft.needMembers())
                {
                    final LogStream logStream = raft.logStream();
                    final PartitionManagementRequest invitationRequest = new PartitionManagementRequest()
                        .topicName(logStream.getTopicName())
                        .partitionId(logStream.getPartitionId())
                        .term(raft.term())
                        .members(raft.configuration().members());

                    final ChannelManager clientChannelManager = context.getClientChannelPool();
                    final TransportConnectionPool connections = context.getConnections();
                    final RequestResponseController requestController = new RequestResponseController(clientChannelManager, connections);
                    requestController.open(copy.managementEndpoint(), invitationRequest);
                    activeRequestController.add(requestController);
                }
            }

        });
    }

    public void addRaft(final Raft raft)
    {
        managementCmdQueue.add(() ->
        {
            context.getLocalPeer().addRaft(raft);
            rafts.add(raft);
        });
    }

    public void removeRaft(final Raft raft)
    {
        final LogStream logStream = raft.logStream();
        final DirectBuffer topicName = logStream.getTopicName();
        final int partitionId = logStream.getPartitionId();

        managementCmdQueue.add(() ->
        {
            for (int i = 0; i < rafts.size(); i++)
            {
                final Raft r = rafts.get(i);
                final LogStream stream = r.logStream();
                if (topicName.equals(stream.getTopicName()) && partitionId == stream.getPartitionId())
                {
                    context.getLocalPeer().removeRaft(raft);
                    rafts.remove(i);
                    break;
                }
            }
        });
    }

    public void createRaft(LogStream logStream, List<Member> members, boolean bootstrap)
    {
        final FsLogStorage logStorage = (FsLogStorage) logStream.getLogStorage();
        final String path = logStorage.getConfig().getPath();

        final MetaStore meta = new MetaStore(this.config.directory + File.separator + String.format("%s.meta", logStream.getLogName()));
        meta.storeTopicNameAndPartitionIdAndDirectory(logStream.getTopicName(), logStream.getPartitionId(), path);

        createRaft(logStream, meta, members, bootstrap);
    }

    public void createRaft(LogStream logStream, MetaStore meta, List<Member> members, boolean bootstrap)
    {
        final String logName = logStream.getLogName();
        final String component = String.format("raft.%s", logName);

        final TransportConnectionPoolService transportConnectionPoolService = new TransportConnectionPoolService();

        final ServiceName<TransportConnectionPool> transportConnectionPoolServiceName = transportConnectionPoolName(component);
        serviceContainer.createService(transportConnectionPoolServiceName, transportConnectionPoolService)
            .dependency(TRANSPORT, transportConnectionPoolService.getTransportInjector())
            .install();

        // TODO: make it configurable
        final ClientChannelManagerService clientChannelManagerService = new ClientChannelManagerService(128);
        final ServiceName<ChannelManager> clientChannelManagerServiceName = clientChannelManagerName(component);
        serviceContainer.createService(clientChannelManagerServiceName, clientChannelManagerService)
            .dependency(TRANSPORT, clientChannelManagerService.getTransportInjector())
            .dependency(transportConnectionPoolServiceName, clientChannelManagerService.getTransportConnectionPoolInjector())
            .dependency(serverSocketBindingReceiveBufferName(REPLICATION_SOCKET_BINDING_NAME), clientChannelManagerService.getReceiveBufferInjector())
            .install();

        final SubscriptionService subscriptionService = new SubscriptionService();
        final ServiceName<Subscription> subscriptionServiceName = subscriptionServiceName(component);
        serviceContainer.createService(subscriptionServiceName, subscriptionService)
            .dependency(serverSocketBindingReceiveBufferName(REPLICATION_SOCKET_BINDING_NAME), subscriptionService.getReceiveBufferInjector())
            .install();

        // TODO: provide raft configuration
        final RaftContextService raftContextService = new RaftContextService(serviceContainer);
        final ServiceName<RaftContext> raftContextServiceName = raftContextServiceName(logName);
        serviceContainer.createService(raftContextServiceName, raftContextService)
            .dependency(PEER_LOCAL_SERVICE, raftContextService.getLocalPeerInjector())
            .dependency(TRANSPORT_SEND_BUFFER, raftContextService.getSendBufferInjector())
            .dependency(clientChannelManagerServiceName, raftContextService.getClientChannelManagerInjector())
            .dependency(transportConnectionPoolServiceName, raftContextService.getTransportConnectionPoolInjector())
            .dependency(subscriptionServiceName, raftContextService.getSubscriptionInjector())
            .dependency(AGENT_RUNNER_SERVICE, raftContextService.getAgentRunnerInjector())
            .install();

        final ServiceName<Raft> raftServiceName = raftServiceName(logName);
        final RaftService raftService = new RaftService(logStream, meta, new CopyOnWriteArrayList<>(members), bootstrap);
        serviceContainer.createService(raftServiceName, raftService)
            .group(RAFT_SERVICE_GROUP)
            .dependency(AGENT_RUNNER_SERVICE, raftService.getAgentRunnerInjector())
            .dependency(raftContextServiceName, raftService.getRaftContextInjector())
            .install();
    }

    public int onPartitionManagementRequest(
            final DirectBuffer buffer,
            final int offset,
            final int length,
            final int channelId,
            final long connectionId,
            final long requestId)
    {
        partitionManagementRequest.reset();
        partitionManagementRequest.wrap(buffer, offset, length);

        final DirectBuffer topicName = partitionManagementRequest.topicName();
        final int partitionId = partitionManagementRequest.partitionId();

        switch (partitionManagementRequest.opCode())
        {
            case INVITE:
            {
                final LogStreamsFactory logStreamFactory = context.getLogStreamsFactory();
                final LogStream logStream = logStreamFactory.createLogStream(topicName, partitionId);

                createRaft(logStream, new ArrayList<>(partitionManagementRequest.members()), false);
                break;
            }
            case BOOTSTRAP:
            {
                final LogStreamsFactory logStreamFactory = context.getLogStreamsFactory();
                final LogStream logStream = logStreamFactory.createLogStream(topicName, partitionId);

                createRaft(logStream, Collections.emptyList(), true);
                break;
            }

            default:
                break;
        }

        partitionManagementResponse.reset();
        messageWriter.protocol(Protocols.REQUEST_RESPONSE)
            .channelId(channelId)
            .connectionId(connectionId)
            .requestId(requestId)
            .message(partitionManagementResponse)
            .tryWriteMessage();

        return FragmentHandler.CONSUME_FRAGMENT_RESULT;
    }

    /**
     * Creates a new partition for the given topic
     *
     */
    public CompletableFuture<Void> createTopicPartition(DirectBuffer topicName, int partitionId)
    {
        final CompletableFuture<Void> createFuture = new CompletableFuture<>();

        managementCmdQueue.add(() ->
        {
            // TODO: currently all partitions are created locally.Should better assign assign some random or round-robin peer
            // to create the first partition.

            try
            {
                final LogStreamsFactory logStreamsFactory = context.getLogStreamsFactory();
                final LogStream logStream = logStreamsFactory.createLogStream(topicName, partitionId);

                createRaft(logStream, Collections.emptyList(), true);

                createFuture.complete(null);
            }
            catch (Exception e)
            {
                createFuture.completeExceptionally(e);
            }
        });

        return createFuture;
    }

    public CompletableFuture<Void> removeTopicPartition(DirectBuffer topicName, int partitionId)
    {
        final CompletableFuture<Void> createFuture = new CompletableFuture<>();

        managementCmdQueue.add(() ->
        {
            createFuture.complete(null);
        });

        return createFuture;
    }

}
