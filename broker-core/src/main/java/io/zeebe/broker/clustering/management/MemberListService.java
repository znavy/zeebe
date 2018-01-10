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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.zeebe.broker.Loggers;
import io.zeebe.gossip.membership.Member;
import io.zeebe.raft.Raft;
import io.zeebe.raft.state.RaftState;
import io.zeebe.servicecontainer.Service;
import io.zeebe.servicecontainer.ServiceStartContext;
import io.zeebe.servicecontainer.ServiceStopContext;
import io.zeebe.transport.SocketAddress;
import org.agrona.DirectBuffer;

/**
 *
 */
public class MemberListService implements Service<MemberListService>
{
    private final List<MemberRaftComposite> compositeList = new ArrayList<>();

    public void add(Member member)
    {
        compositeList.add(new MemberRaftComposite(member));
    }

    public void addRaft(Raft raft)
    {
        for (MemberRaftComposite memberRaftComposite : compositeList)
        {
            if (memberRaftComposite.getReplicationApi().equals(raft.getSocketAddress()))
            {
                final int partitionId = raft.getLogStream()
                                            .getPartitionId();
                final DirectBuffer topicName = raft.getLogStream().getTopicName();
                final RaftState raftState = raft.getState();
                memberRaftComposite.updateRaft(partitionId, topicName, raftState);
            }
        }

        Loggers.CLUSTERING_LOGGER.debug("Add raft - state: {}", this);
    }

    public MemberRaftComposite getMember(SocketAddress socketAddress)
    {
        for (MemberRaftComposite memberRaftComposite : compositeList)
        {
            if (memberRaftComposite.getManagementApi()
                                   .equals(socketAddress))
            {
                return memberRaftComposite;
            }
        }
        return null;
    }

    public List<RaftStateComposite> getRafts(SocketAddress socketAddress)
    {
        for (MemberRaftComposite memberRaftComposite : compositeList)
        {
            if (memberRaftComposite.getManagementApi()
                                   .equals(socketAddress))
            {
                return memberRaftComposite.getRafts();
            }
        }
        return null;
    }

    public void setApis(SocketAddress clientApi,
                        SocketAddress replicationApi,
                        SocketAddress managementApi)
    {
        for (MemberRaftComposite memberRaftComposite : compositeList)
        {
            if (memberRaftComposite.getMember().getAddress().equals(managementApi))
            {
                memberRaftComposite.setManagementApi(managementApi);
                memberRaftComposite.setReplicationApi(replicationApi);
                memberRaftComposite.setClientApi(clientApi);
            }
        }
    }

    public void remove(Member member)
    {
        compositeList.remove(member);
    }

    public Iterator<MemberRaftComposite> iterator()
    {
        return compositeList.iterator();
    }

    @Override
    public void start(ServiceStartContext serviceStartContext)
    {

    }

    @Override
    public void stop(ServiceStopContext serviceStopContext)
    {

    }

    @Override
    public MemberListService get()
    {
        return this;
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        for (MemberRaftComposite memberRaftComposite : compositeList)
        {
            builder.append(memberRaftComposite.toString()).append("\n");
        }
        return builder.toString();
    }
}
