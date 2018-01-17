package io.zeebe.broker.clustering.management.memberList;

import static io.zeebe.broker.clustering.management.ClusteringHelper.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.zeebe.broker.Loggers;
import io.zeebe.broker.clustering.management.ClusterManagerContext;
import io.zeebe.broker.transport.cfg.TransportComponentCfg;
import io.zeebe.gossip.Gossip;
import io.zeebe.gossip.GossipCustomEventListener;
import io.zeebe.gossip.GossipMembershipListener;
import io.zeebe.gossip.membership.Member;
import io.zeebe.raft.RaftStateListener;
import io.zeebe.raft.state.RaftState;
import io.zeebe.transport.SocketAddress;
import io.zeebe.util.DeferredCommandContext;
import io.zeebe.util.buffer.BufferUtil;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;

public class ClusterMemberListManager implements RaftStateListener
{
    public static final Logger LOG = Loggers.CLUSTERING_LOGGER;
    public static final DirectBuffer API_EVENT_TYPE = BufferUtil.wrapString("apis");
    public static final DirectBuffer PARTITION_EVENT_TYPE = BufferUtil.wrapString("partition");

    private final ClusterManagerContext context;
    private TransportComponentCfg transportComponentCfg;
    private final DeferredCommandContext clusterManagerCmdQueue;
    private final List<MemberRaftComposite> deadMembers;
    private final Consumer<SocketAddress> updatedMemberConsumer;

    public ClusterMemberListManager(ClusterManagerContext context, TransportComponentCfg transportComponentCfg, Consumer<SocketAddress> updatedMemberConsumer)
    {
        this.context = context;
        this.deadMembers = new ArrayList<>();
        this.clusterManagerCmdQueue = new DeferredCommandContext();
        this.transportComponentCfg = transportComponentCfg;
        this.updatedMemberConsumer = updatedMemberConsumer;

        final MemberListService memberListService = context.getMemberListService();
        memberListService.add(new Member(transportComponentCfg.managementApi.toSocketAddress()));
        memberListService.setApis(transportComponentCfg.clientApi.toSocketAddress(), transportComponentCfg.replicationApi.toSocketAddress(),
                                  transportComponentCfg.managementApi.toSocketAddress());

        context.getGossip()
               .addMembershipListener(new MembershipListener());
        context.getGossip()
               .addCustomEventListener(API_EVENT_TYPE, new APIEventListener());
        context.getGossip()
               .addCustomEventListener(PARTITION_EVENT_TYPE, new PartitionEventListener());

        // sync handlers
        context.getGossip()
               .registerSyncRequestHandler(API_EVENT_TYPE, new APISyncHandler(clusterManagerCmdQueue, context));
        context.getGossip()
               .registerSyncRequestHandler(PARTITION_EVENT_TYPE, new RaftStateSyncHandler(clusterManagerCmdQueue, context));

        //        topologyCreator = new TopologyCreator(clusterManagerCmdQueue, context);
    }

    public int doWork()
    {
        return clusterManagerCmdQueue.doWork();
    }

    public void publishNodeAPIAddresses()
    {
        final Gossip gossip = context.getGossip();
        final DirectBuffer payload = writeAPIAddressesIntoBuffer(transportComponentCfg.managementApi.toSocketAddress(),
                                                                 transportComponentCfg.replicationApi.toSocketAddress(),
                                                                 transportComponentCfg.clientApi.toSocketAddress());
        gossip.publishEvent(API_EVENT_TYPE, payload);
    }

    private class MembershipListener implements GossipMembershipListener
    {
        @Override
        public void onAdd(Member member)
        {
            final MemberRaftComposite newMember = new MemberRaftComposite(member);
            clusterManagerCmdQueue.runAsync(() ->
            {
                LOG.debug("Add member {} to member list.", newMember);
                MemberRaftComposite memberRaftComposite = newMember;
                final int indexOfDeadMember = deadMembers.indexOf(newMember);

                if (indexOfDeadMember > -1)
                {
                    memberRaftComposite = deadMembers.remove(indexOfDeadMember);
                    LOG.debug("Re-add dead member {} to member list", memberRaftComposite);
                }
                context.getMemberListService()
                       .add(memberRaftComposite);

            });
        }

        @Override
        public void onRemove(Member member)
        {
            final SocketAddress memberAddress = member.getAddress();
            clusterManagerCmdQueue.runAsync(() ->
            {
                final MemberRaftComposite removedMember = context.getMemberListService()
                                                                 .remove(memberAddress);
                LOG.debug("Remove member {} from member list.", removedMember);
                deadMembers.add(removedMember);
            });
        }
    }

    private final class APIEventListener implements GossipCustomEventListener
    {
        @Override
        public void onEvent(SocketAddress socketAddress, DirectBuffer directBuffer)
        {

            final DirectBuffer savedBuffer = BufferUtil.cloneBuffer(directBuffer);
            final SocketAddress savedSocketAddress = new SocketAddress(socketAddress);
            clusterManagerCmdQueue.runAsync(() ->
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

                final boolean success = context.getMemberListService()
                                               .setApis(clientApi, replicationApi, managementApi);

                LOG.debug("Setting API's for member {} was {}successful.", savedSocketAddress, success ? "" : "not ");

                updatedMemberConsumer.accept(savedSocketAddress);
            });
        }
    }

    private final class PartitionEventListener implements GossipCustomEventListener
    {
        @Override
        public void onEvent(SocketAddress socketAddress, DirectBuffer directBuffer)
        {
            final DirectBuffer savedBuffer = BufferUtil.cloneBuffer(directBuffer);
            final SocketAddress savedSocketAddress = new SocketAddress(socketAddress);
            clusterManagerCmdQueue.runAsync(() ->
            {
                LOG.debug("Received raft state change event for member {}", savedSocketAddress);
                final MemberRaftComposite member = context.getMemberListService()
                                                          .getMember(savedSocketAddress);

                if (member == null)
                {
                    LOG.debug("Member {} does not exist. Maybe dead? List of dead members: {}", savedSocketAddress, deadMembers);
                }
                else
                {
                    updateMemberWithNewRaftState(member, savedBuffer);

                    LOG.debug("Handled raft state change event for member {} - local member state: {}", savedSocketAddress, context.getMemberListService());
                }
            });
        }
    }

    @Override
    public void onStateChange(int partitionId, DirectBuffer topicName, SocketAddress socketAddress, RaftState raftState)
    {
        final DirectBuffer savedTopicName = BufferUtil.cloneBuffer(topicName);
        clusterManagerCmdQueue.runAsync(() ->
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
                           .publishEvent(PARTITION_EVENT_TYPE, payload);

                    break;
                }
                default:
                    break;
            }
        });
    }
}
