package io.zeebe.broker.clustering.management;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.zeebe.gossip.membership.Member;
import io.zeebe.raft.Raft;
import io.zeebe.servicecontainer.Service;
import io.zeebe.servicecontainer.ServiceStartContext;
import io.zeebe.servicecontainer.ServiceStopContext;
import io.zeebe.transport.SocketAddress;

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
                memberRaftComposite.addRaft(raft);
            }
        }
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


}
