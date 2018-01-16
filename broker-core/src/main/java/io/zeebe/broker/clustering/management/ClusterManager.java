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
package io.zeebe.broker.clustering.management;

import static io.zeebe.broker.clustering.ClusterServiceNames.RAFT_SERVICE_GROUP;
import static io.zeebe.broker.clustering.ClusterServiceNames.raftServiceName;
import static io.zeebe.broker.clustering.management.ClusteringHelper.*;
import static io.zeebe.broker.system.SystemServiceNames.ACTOR_SCHEDULER_SERVICE;
import static org.agrona.BitUtil.SIZE_OF_BYTE;
import static org.agrona.BitUtil.SIZE_OF_INT;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import io.zeebe.broker.Loggers;
import io.zeebe.broker.clustering.handler.BrokerAddress;
import io.zeebe.broker.clustering.handler.Topology;
import io.zeebe.broker.clustering.management.handler.ClusterManagerFragmentHandler;
import io.zeebe.broker.clustering.management.message.CreatePartitionMessage;
import io.zeebe.broker.clustering.management.message.InvitationRequest;
import io.zeebe.broker.clustering.management.message.InvitationResponse;
import io.zeebe.broker.clustering.raft.RaftPersistentFileStorage;
import io.zeebe.broker.clustering.raft.RaftService;
import io.zeebe.broker.logstreams.LogStreamsManager;
import io.zeebe.broker.transport.TransportServiceNames;
import io.zeebe.broker.transport.cfg.SocketBindingCfg;
import io.zeebe.broker.transport.cfg.TransportComponentCfg;
import io.zeebe.gossip.Gossip;
import io.zeebe.gossip.GossipCustomEventListener;
import io.zeebe.gossip.GossipMembershipListener;
import io.zeebe.gossip.GossipSyncRequestHandler;
import io.zeebe.gossip.dissemination.GossipSyncRequest;
import io.zeebe.gossip.membership.Member;
import io.zeebe.logstreams.impl.log.fs.FsLogStorage;
import io.zeebe.logstreams.log.LogStream;
import io.zeebe.msgpack.property.ArrayProperty;
import io.zeebe.msgpack.value.ValueArray;
import io.zeebe.protocol.Protocol;
import io.zeebe.raft.Raft;
import io.zeebe.raft.RaftPersistentStorage;
import io.zeebe.raft.RaftStateListener;
import io.zeebe.raft.state.RaftState;
import io.zeebe.servicecontainer.ServiceContainer;
import io.zeebe.servicecontainer.ServiceName;
import io.zeebe.transport.*;
import io.zeebe.util.DeferredCommandContext;
import io.zeebe.util.actor.Actor;
import io.zeebe.util.buffer.BufferReader;
import io.zeebe.util.buffer.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;

public class ClusterManager implements Actor, RaftStateListener
{

    public static final DirectBuffer TYPE_BUFFER = BufferUtil.wrapString("apis");
    public static final DirectBuffer PARTITION_TYPE = BufferUtil.wrapString("partition");

    public static final Logger LOG = Loggers.CLUSTERING_LOGGER;

    private final ClusterManagerContext context;
    private final ServiceContainer serviceContainer;

    private final List<Raft> rafts;
    private final List<StartLogStreamServiceController> startLogStreamServiceControllers;

    private final DeferredCommandContext commandQueue;

    private final List<RequestResponseController> activeRequestControllers;

    private final InvitationRequest invitationRequest;
    private final InvitationResponse invitationResponse;
    private final CreatePartitionMessage createPartitionMessage = new CreatePartitionMessage();

    private TransportComponentCfg transportComponentCfg;

    private final ServerResponse response = new ServerResponse();
    private final ServerInputSubscription inputSubscription;

    private final LogStreamsManager logStreamsManager;
    private final List<MemberRaftComposite> deadMembers;

    public ClusterManager(final ClusterManagerContext context, final ServiceContainer serviceContainer, final TransportComponentCfg transportComponentCfg)
    {
        this.context = context;
        this.serviceContainer = serviceContainer;
        this.transportComponentCfg = transportComponentCfg;
        this.rafts = new CopyOnWriteArrayList<>();
        this.startLogStreamServiceControllers = new CopyOnWriteArrayList<>();
        this.commandQueue = new DeferredCommandContext();
        this.activeRequestControllers = new CopyOnWriteArrayList<>();
        this.invitationRequest = new InvitationRequest();
        this.logStreamsManager = context.getLogStreamsManager();

        this.invitationResponse = new InvitationResponse();

        final ClusterManagerFragmentHandler fragmentHandler = new ClusterManagerFragmentHandler(this, context.getWorkflowRequestMessageHandler());
        inputSubscription = context.getServerTransport()
                                   .openSubscription("cluster-management", fragmentHandler, fragmentHandler)
                                   .join();

        final MemberListService memberListService = context.getMemberListService();
        memberListService.add(new Member(transportComponentCfg.managementApi.toSocketAddress()));
        memberListService.setApis(transportComponentCfg.clientApi.toSocketAddress(), transportComponentCfg.replicationApi.toSocketAddress(),
                                  transportComponentCfg.managementApi.toSocketAddress());

        deadMembers = new ArrayList<>();

        context.getGossip().addMembershipListener(new MembershipListener());
        context.getGossip()
               .addCustomEventListener(TYPE_BUFFER, new APIEventListener());
        context.getGossip()
               .addCustomEventListener(PARTITION_TYPE, new RaftUpdateListener());

        // sync handlers
        context.getGossip().registerSyncRequestHandler(TYPE_BUFFER, new APISyncHandler());
        context.getGossip().registerSyncRequestHandler(PARTITION_TYPE, new RaftStateSyncHandler());
    }

    public void open()
    {
        final Gossip gossip = context.getGossip();
        final DirectBuffer payload = writeAPIAddressesIntoBuffer(transportComponentCfg.managementApi.toSocketAddress(),
                                                                 transportComponentCfg.replicationApi.toSocketAddress(),
                                                                 transportComponentCfg.clientApi.toSocketAddress());
        gossip.publishEvent(TYPE_BUFFER, payload);

        final LogStreamsManager logStreamManager = context.getLogStreamsManager();

        final File storageDirectory = new File(transportComponentCfg.management.directory);

        if (!storageDirectory.exists())
        {
            try
            {
                storageDirectory.getParentFile()
                                .mkdirs();
                Files.createDirectory(storageDirectory.toPath());
            }
            catch (final IOException e)
            {
                LOG.error("Unable to create directory {}", storageDirectory, e);
            }
        }

        final SocketBindingCfg replicationApi = transportComponentCfg.replicationApi;
        final SocketAddress socketAddress = new SocketAddress(replicationApi.host, replicationApi.port);
        final File[] storageFiles = storageDirectory.listFiles();

        if (storageFiles != null && storageFiles.length > 0)
        {
            for (int i = 0; i < storageFiles.length; i++)
            {
                final File storageFile = storageFiles[i];
                final RaftPersistentFileStorage storage = new RaftPersistentFileStorage(storageFile.getAbsolutePath());

                final DirectBuffer topicName = storage.getTopicName();
                final int partitionId = storage.getPartitionId();

                LogStream logStream = logStreamManager.getLogStream(partitionId);

                if (logStream == null)
                {
                    final String directory = storage.getLogDirectory();
                    logStream = logStreamManager.createLogStream(topicName, partitionId, directory);
                }

                storage.setLogStream(logStream);

                createRaft(socketAddress, logStream, storage.getMembers(), storage);
            }
        }
        else
        {
            if (transportComponentCfg.gossip.initialContactPoints.length == 0)
            {
                LOG.debug("Broker bootstraps the system topic");
                createPartition(Protocol.SYSTEM_TOPIC_BUF, Protocol.SYSTEM_PARTITION);
            }
        }
    }

    @Override
    public String name()
    {
        return "management";
    }

    @Override
    public int doWork()
    {
        int workcount = 0;

        workcount += commandQueue.doWork();
        workcount += inputSubscription.poll();

        int i = 0;
        while (i < activeRequestControllers.size())
        {
            final RequestResponseController requestController = activeRequestControllers.get(i);
            workcount += requestController.doWork();

            if (requestController.isFailed() || requestController.isResponseAvailable())
            {
                requestController.close();
            }

            if (requestController.isClosed())
            {
                activeRequestControllers.remove(i);
            }
            else
            {
                i++;
            }
        }

        for (int j = 0; j < startLogStreamServiceControllers.size(); j++)
        {
            workcount += startLogStreamServiceControllers.get(j)
                                                         .doWork();
        }

        return workcount;
    }

    protected void inviteMemberToRaft(Raft raft, SocketAddress member)
    {
        // TODO(menski): implement replication factor
        // TODO: if this should be garbage free, we have to limit
        // the number of concurrent invitations.
        final List<SocketAddress> members = new ArrayList<>();
        members.add(raft.getSocketAddress());
        raft.getMembers()
            .forEach(raftMember -> members.add(raftMember.getRemoteAddress()
                                                         .getAddress()));

        final LogStream logStream = raft.getLogStream();
        final InvitationRequest invitationRequest = new InvitationRequest().topicName(logStream.getTopicName())
                                                                           .partitionId(logStream.getPartitionId())
                                                                           .term(raft.getTerm())
                                                                           .members(members);

        LOG.debug("Send invitation request to {} for partition {} in term {}", member, logStream.getPartitionId(), raft.getTerm());

        final RequestResponseController requestController = new RequestResponseController(context.getClientTransport());

        requestController.open(member, invitationRequest, new BufferReader()
        {
            @Override
            public void wrap(DirectBuffer buffer, int offset, int length)
            {
                LOG.debug("Got invitation response from {} for partition id {}.", member, logStream.getPartitionId());
            }
        });
        activeRequestControllers.add(requestController);
    }

    public void addRaft(final ServiceName<Raft> raftServiceName, final Raft raft)
    {
        // this must be determined before we cross the async boundary to avoid race conditions
        final boolean isRaftCreator = raft.getMemberSize() == 0;

        raft.registerRaftStateListener(this);

        commandQueue.runAsync(() ->
        {

            LOG.trace("ADD raft {} for partition {} state {}.", raft.getSocketAddress(), raft.getLogStream().getPartitionId(), raft.getState());
            rafts.add(raft);

            // add raft only when member or candidate
            context.getMemberListService().addRaft(raft);

            startLogStreamServiceControllers.add(new StartLogStreamServiceController(raftServiceName, raft, serviceContainer));

            if (isRaftCreator)
            {
                final Iterator<MemberRaftComposite> iterator = context.getMemberListService().iterator();
                while (iterator.hasNext())
                {
                    final MemberRaftComposite next = iterator.next();
                    if (!next.getMember()
                             .getAddress()
                             .equals(transportComponentCfg.managementApi.toSocketAddress()))
                    {
                        // TODO don't invite all members to raft
                        inviteMemberToRaft(raft, next.getMember().getAddress());
                    }
                }
            }
        });
    }

    public void removeRaft(final Raft raft)
    {
        final LogStream logStream = raft.getLogStream();
        final int partitionId = logStream.getPartitionId();

        commandQueue.runAsync(() ->
        {
            for (int i = 0; i < rafts.size(); i++)
            {
                final Raft r = rafts.get(i);
                final LogStream stream = r.getLogStream();
                if (partitionId == stream.getPartitionId())
                {
                    rafts.remove(i);
                    break;
                }
            }

            for (int i = 0; i < startLogStreamServiceControllers.size(); i++)
            {
                final Raft r = startLogStreamServiceControllers.get(i)
                                                               .getRaft();
                final LogStream stream = r.getLogStream();
                if (partitionId == stream.getPartitionId())
                {
                    startLogStreamServiceControllers.remove(i);
                    break;
                }
            }
        });
    }

    public void createRaft(final SocketAddress socketAddress, final LogStream logStream, final List<SocketAddress> members)
    {
        final FsLogStorage logStorage = (FsLogStorage) logStream.getLogStorage();
        final String path = logStorage.getConfig()
                                      .getPath();

        final String directory = transportComponentCfg.management.directory;
        final RaftPersistentFileStorage storage = new RaftPersistentFileStorage(String.format("%s%s.meta", directory, logStream.getLogName()));
        storage.setLogStream(logStream)
               .setLogDirectory(path)
               .save();

        createRaft(socketAddress, logStream, members, storage);
    }

    public void createRaft(final SocketAddress socketAddress, final LogStream logStream, final List<SocketAddress> members,
                           final RaftPersistentStorage persistentStorage)
    {
        final RaftService raftService = new RaftService(socketAddress, logStream, members, persistentStorage);

        final ServiceName<Raft> raftServiceName = raftServiceName(logStream.getLogName());

        serviceContainer.createService(raftServiceName, raftService)
                        .group(RAFT_SERVICE_GROUP)
                        .dependency(ACTOR_SCHEDULER_SERVICE, raftService.getActorSchedulerInjector())
                        .dependency(TransportServiceNames.bufferingServerTransport(TransportServiceNames.REPLICATION_API_SERVER_NAME),
                                    raftService.getServerTransportInjector())
                        .dependency(TransportServiceNames.clientTransport(TransportServiceNames.REPLICATION_API_CLIENT_NAME),
                                    raftService.getClientTransportInjector())
                        .install();
    }

    protected boolean partitionExists(int partitionId)
    {
        return logStreamsManager.hasLogStream(partitionId);
    }

    /**
     * Creates log stream and sets up raft service to participate in raft group
     */
    protected void createPartition(DirectBuffer topicName, int partitionId)
    {
        createPartition(topicName, partitionId, Collections.emptyList());
    }

    /**
     * Creates log stream and sets up raft service to participate in raft group
     */
    protected void createPartition(DirectBuffer topicName, int partitionId, List<SocketAddress> members)
    {
        final LogStream logStream = logStreamsManager.createLogStream(topicName, partitionId);

        final SocketBindingCfg replicationApi = transportComponentCfg.replicationApi;
        final SocketAddress socketAddress = new SocketAddress(replicationApi.host, replicationApi.port);
        createRaft(socketAddress, logStream, members);
    }

    public boolean onInvitationRequest(final DirectBuffer buffer, final int offset, final int length, final ServerOutput output,
                                       final RemoteAddress requestAddress, final long requestId)
    {
        invitationRequest.reset();
        invitationRequest.wrap(buffer, offset, length);

        LOG.debug("Received invitation request from {} for partition {}", requestAddress.getAddress(), invitationRequest.partitionId());

        final DirectBuffer topicName = invitationRequest.topicName();
        final int partitionId = invitationRequest.partitionId();

        createPartition(topicName, partitionId, new ArrayList<>(invitationRequest.members()));

        invitationResponse.reset();
        response.reset()
                .remoteAddress(requestAddress)
                .requestId(requestId)
                .writer(invitationResponse);

        return output.sendResponse(response);
    }

    public void onCreatePartitionMessage(final DirectBuffer buffer, final int offset, final int length)
    {
        createPartitionMessage.wrap(buffer, offset, length);

        LOG.debug("Received create partition message for partition {}", createPartitionMessage.getPartitionId());

        final int partitionId = createPartitionMessage.getPartitionId();

        if (!partitionExists(partitionId))
        {
            LOG.debug("Creating partition {}", createPartitionMessage.getPartitionId());
            createPartition(createPartitionMessage.getTopicName(), partitionId);
        }
        else
        {
            LOG.debug("Partition {} exists already. Ignoring creation request.", createPartitionMessage.getPartitionId());
        }
    }
//
//    public MemberRaftComposite resolveMember(SocketAddress memberAddress)
//    {
//        MemberRaftComposite memberRaftComposite = new MemberRaftComposite(new Member(memberAddress));
//        final int indexOfDeadMember = deadMembers.indexOf(memberRaftComposite);
//        if (indexOfDeadMember == -1)
//        {
//            memberRaftComposite = context.getMemberListService().getMember(memberAddress);
//        }
//        else
//        {
//            memberRaftComposite = deadMembers.get(indexOfDeadMember);
//        }
//        return memberRaftComposite;
//    }

    private class MembershipListener implements GossipMembershipListener
    {
        @Override
        public void onAdd(Member member)
        {
            final MemberRaftComposite newMember = new MemberRaftComposite(member);
            commandQueue.runAsync(() ->
            {
                LOG.debug("Add member {} to member list.", newMember);
                MemberRaftComposite memberRaftComposite = newMember;
                final int indexOfDeadMember = deadMembers.indexOf(newMember);

                if (indexOfDeadMember > -1)
                {
                    memberRaftComposite = deadMembers.remove(indexOfDeadMember);
                    LOG.debug("Re-add dead member {} to member list", memberRaftComposite);
                }
                context.getMemberListService().add(memberRaftComposite);

//                for (Raft raft : rafts)
//                {
//                    if (raft.getState() == RaftState.LEADER)
//                    {
//                        inviteMemberToRaft(raft, member.getAddress());
//                    }
//                }
            });
        }

        @Override
        public void onRemove(Member member)
        {
            final SocketAddress memberAddress = member.getAddress();
            commandQueue.runAsync(() ->
            {
                final MemberRaftComposite removedMember = context.getMemberListService()
                                                                 .remove(memberAddress);
                LOG.debug("Remove member {} from member list.", removedMember);
                deadMembers.add(removedMember);
                //                for (Raft raft : rafts)
//                {
                // should check replication port?
//                    if (raft.getSocketAddress().equals(member.getAddress()))
//                    {
//                        removeRaft(raft);
//                    }
//                }
            });
        }
    }


    public CompletableFuture<Topology> requestTopology()
    {
        return commandQueue.runAsync((future) ->
        {
            LOG.debug("Received topology request.");
            final Iterator<MemberRaftComposite> iterator = context.getMemberListService()
                                                                  .iterator();
            final Topology topology = new Topology();
            while (iterator.hasNext())
            {
                final MemberRaftComposite next = iterator.next();

                final ValueArray<BrokerAddress> brokers = topology.brokers();

                final SocketAddress clientApi = next.getClientApi();

                if (clientApi != null)
                {
                    brokers.add()
                           .setHost(clientApi.getHostBuffer(), 0, clientApi.hostLength())
                           .setPort(clientApi.port());

                    final Iterator<RaftStateComposite> raftTupleIt = next.getRaftIterator();
                    while (raftTupleIt.hasNext())
                    {
                        final RaftStateComposite nextRaftState = raftTupleIt.next();

                        if (nextRaftState.getRaftState() == RaftState.LEADER)
                        {
                            final DirectBuffer directBuffer = BufferUtil.cloneBuffer(nextRaftState.getTopicName());

                            topology.topicLeaders()
                                    .add()
                                    .setHost(clientApi.getHostBuffer(), 0, clientApi.hostLength())
                                    .setPort(clientApi.port())
                                    .setTopicName(directBuffer, 0, directBuffer.capacity())
                                    .setPartitionId(nextRaftState.getPartition());

                        }
                    }
                }
            }

//            LOG.debug("Send topology {} as response.", topology);
            future.complete(topology);
        });
    }

    private final class APIEventListener implements GossipCustomEventListener
    {
        @Override
        public void onEvent(SocketAddress socketAddress, DirectBuffer directBuffer)
        {

            final DirectBuffer savedBuffer = BufferUtil.cloneBuffer(directBuffer);
            final SocketAddress savedSocketAddress = new SocketAddress(socketAddress);
            commandQueue.runAsync(() ->
            {
                LOG.debug("Received API event from member {}.", savedSocketAddress);

                final SocketAddress managementApi = new SocketAddress();
                final SocketAddress clientApi = new SocketAddress();
                final SocketAddress replicationApi = new SocketAddress();

                int offset = 0;
                // management
                offset = readFromBufferIntoSocketAddress(offset, savedBuffer, managementApi);
                // client
                offset = readFromBufferIntoSocketAddress(offset, savedBuffer, clientApi);
                // replication
                readFromBufferIntoSocketAddress(offset, savedBuffer, replicationApi);

//                context.getMemberListService()
//                       .add(new Member(savedSocketAddress));
                final boolean success = context.getMemberListService()
                                         .setApis(clientApi, replicationApi, managementApi);

                LOG.debug("Setting API's for member {} was {}successful.", savedSocketAddress, success ? "" : "not ");
                LOG.debug("Send raft invitations to member {}.", savedSocketAddress);
                for (Raft raft : rafts)
                {
                    if (raft.getState() == RaftState.LEADER)
                    {
                        // TODO don't invite all members
                        inviteMemberToRaft(raft, savedSocketAddress);
                    }
                }
            });
        }
    }

    private final class RaftUpdateListener implements GossipCustomEventListener
    {
        @Override
        public void onEvent(SocketAddress socketAddress, DirectBuffer directBuffer)
        {
            final DirectBuffer savedBuffer = BufferUtil.cloneBuffer(directBuffer);
            final SocketAddress savedSocketAddress = new SocketAddress(socketAddress);
            commandQueue.runAsync(() ->
            {
                LOG.debug("Received raft state change event for member {}", savedSocketAddress);
                final MemberRaftComposite member = context.getMemberListService()
                                                          .getMember(savedSocketAddress);

                if (member == null)
                {
                    LOG.debug("Member {} does not exist. Maybe dead? List of dead members: {}", savedSocketAddress, deadMembers);
                    return;
                }

                int offset = 0;
                final int count = savedBuffer.getInt(offset, ByteOrder.LITTLE_ENDIAN);
                offset += SIZE_OF_INT;

                for (int i = 0; i < count; i++)
                {
                    final int partition = savedBuffer.getInt(offset, ByteOrder.LITTLE_ENDIAN);
                    offset += SIZE_OF_INT;

                    final int topicNameLength = savedBuffer.getInt(offset, ByteOrder.LITTLE_ENDIAN);
                    offset += SIZE_OF_INT;

                    final MutableDirectBuffer topicBuffer = new UnsafeBuffer(new byte[topicNameLength]);
                    savedBuffer.getBytes(offset, topicBuffer, 0, topicNameLength);
                    offset += topicNameLength;

                    final byte state = savedBuffer.getByte(offset);
                    offset += SIZE_OF_BYTE;

                    member.updateRaft(partition, topicBuffer, state == (byte) 1 ? RaftState.LEADER : RaftState.FOLLOWER);
                }

                LOG.debug("Handled raft state change event for member {} - local member state: {}", savedSocketAddress, context.getMemberListService());
            });
        }
    }

    @Override
    public void onStateChange(int partitionId, DirectBuffer topicName, SocketAddress socketAddress, RaftState raftState)
    {
        final DirectBuffer savedTopicName = BufferUtil.cloneBuffer(topicName);
        commandQueue.runAsync(() ->
        {
            switch (raftState)
            {
                case LEADER:
                case FOLLOWER:
                {

                    final MemberRaftComposite member = context.getMemberListService()
                                                              .getMember(transportComponentCfg.managementApi.toSocketAddress());

                    final List<RaftStateComposite> rafts = member.getRafts();

                    // update raft state in member list
                    member.updateRaft(partitionId, savedTopicName, raftState);

                    LOG.trace("On raft state change for {} - local member states: {}", socketAddress, context.getMemberListService());

                    // send complete list of partition where I'm a follower or leader
                    final DirectBuffer payload = writeRaftsIntoBuffer(rafts);

                    LOG.trace("Publish event for partition {} state change {}", partitionId, raftState);

                    context.getGossip()
                           .publishEvent(PARTITION_TYPE, payload);

                    break;
                }
                default:
                    break;
            }
        });
    }

    private final class APISyncHandler implements GossipSyncRequestHandler
    {
        @Override
        public void onSyncRequest(GossipSyncRequest request)
        {
            commandQueue.runAsync(() ->
            {
                LOG.debug("Got API sync request.");
                final Iterator<MemberRaftComposite> iterator = context.getMemberListService()
                                                                      .iterator();

                while (iterator.hasNext())
                {
                    final MemberRaftComposite next = iterator.next();

                    if (next.hasApis())
                    {
                        final DirectBuffer payload = writeAPIAddressesIntoBuffer(next.getManagementApi(), next.getReplicationApi(), next.getClientApi());
                        request.addPayload(next.getMember()
                                               .getAddress(), payload);
                    }
                }
                request.done();
                LOG.debug("Send API sync response.");
            });
        }
    }

    private final class RaftStateSyncHandler implements GossipSyncRequestHandler
    {
        @Override
        public void onSyncRequest(GossipSyncRequest request)
        {
            commandQueue.runAsync(() ->
            {
                LOG.debug("Got RAFT state sync request.");
                final Iterator<MemberRaftComposite> iterator = context.getMemberListService()
                                                                      .iterator();
                while (iterator.hasNext())
                {
                    final MemberRaftComposite next = iterator.next();

                    final DirectBuffer payload = writeRaftsIntoBuffer(next.getRafts());
                    request.addPayload(next.getMember()
                                           .getAddress(), payload);
                }
                request.done();

                LOG.debug("Send RAFT state sync response.");
            });
        }
    }

}
