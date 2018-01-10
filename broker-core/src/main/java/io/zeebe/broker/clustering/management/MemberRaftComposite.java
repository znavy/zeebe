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

import io.zeebe.gossip.membership.Member;
import io.zeebe.raft.state.RaftState;
import io.zeebe.transport.SocketAddress;
import io.zeebe.util.collection.IntArrayListIterator;
import io.zeebe.util.collection.IntIterator;
import io.zeebe.util.collection.IntTuple;
import org.agrona.collections.IntArrayList;

/**
 *
 */
public class MemberRaftComposite
{
    private final Member member;
    private SocketAddress clientApi;
    private SocketAddress replicationApi;
    private SocketAddress managementApi;

    private final List<IntTuple<RaftState>> rafts;

    MemberRaftComposite(Member member)
    {
        this.member = member;
        this.rafts = new ArrayList<>();
    }

    public Member getMember()
    {
        return member;
    }

    public void addRaft(int partition, RaftState raftState)
    {
        rafts.add(new IntTuple<>(partition, raftState));
    }

    public void updateRaft(int partition, RaftState raftState)
    {
        boolean updated = false;
        for (IntTuple<RaftState> tuple : rafts)
        {
            if (tuple.getInt() == partition)
            {
                // set tuple.getLeft()
                tuple.setRight(raftState);
                updated = true;
                break;
            }
        }

        if (!updated)
        {
            rafts.add(new IntTuple<>(partition, raftState));
        }
    }

    public List<IntTuple<RaftState>> getRafts()
    {
        return rafts;
    }

    public IntIterator getLeadingPartitions()
    {
        final IntArrayList intArrayList = new IntArrayList();
        for (IntTuple<RaftState> raft : rafts)
        {
            if (raft.getRight() == RaftState.LEADER)
            {
                intArrayList.add(raft.getInt());
            }
        }
        final IntArrayListIterator intArrayListIterator = new IntArrayListIterator();
        intArrayListIterator.wrap(intArrayList);
        return intArrayListIterator;
    }

    public Iterator<IntTuple<RaftState>> getRaftIterator()
    {
        return rafts.iterator();
    }

    public SocketAddress getClientApi()
    {
        return clientApi;
    }

    public void setClientApi(SocketAddress clientApi)
    {
        this.clientApi = clientApi;
    }

    public SocketAddress getReplicationApi()
    {
        return replicationApi;
    }

    public void setReplicationApi(SocketAddress replicationApi)
    {
        this.replicationApi = replicationApi;
    }

    public SocketAddress getManagementApi()
    {
        return managementApi;
    }

    public void setManagementApi(SocketAddress managementApi)
    {
        this.managementApi = managementApi;
    }
}
