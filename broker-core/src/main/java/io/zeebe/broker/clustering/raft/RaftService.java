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
package io.zeebe.broker.clustering.raft;

import io.zeebe.broker.Loggers;
import io.zeebe.broker.clustering.management.OnOpenLogStreamListener;
import io.zeebe.broker.logstreams.LogStreamService;
import io.zeebe.broker.logstreams.LogStreamServiceNames;
import io.zeebe.logstreams.log.LogStream;
import io.zeebe.protocol.Protocol;
import io.zeebe.raft.Raft;
import io.zeebe.raft.RaftConfiguration;
import io.zeebe.raft.RaftPersistentStorage;
import io.zeebe.raft.RaftStateListener;
import io.zeebe.raft.state.RaftState;
import io.zeebe.servicecontainer.*;
import io.zeebe.transport.BufferingServerTransport;
import io.zeebe.transport.ClientTransport;
import io.zeebe.transport.SocketAddress;
import io.zeebe.util.buffer.BufferUtil;
import io.zeebe.util.sched.Actor;
import io.zeebe.util.sched.ActorScheduler;
import io.zeebe.util.sched.future.ActorFuture;
import io.zeebe.util.sched.future.CompletableActorFuture;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;

import java.util.List;

import static io.zeebe.broker.clustering.ClusterServiceNames.CLUSTER_MANAGER_SERVICE;
import static io.zeebe.broker.logstreams.LogStreamServiceNames.logStreamServiceName;
import static io.zeebe.broker.system.SystemServiceNames.ACTOR_SCHEDULER_SERVICE;

public class RaftService extends Actor implements Service<Raft>, RaftStateListener
{
    private static final Logger LOG = Loggers.SERVICES_LOGGER;

    private final RaftConfiguration configuration;
    private final SocketAddress socketAddress;
    private final LogStream logStream;
    private final List<SocketAddress> members;
    private final RaftPersistentStorage persistentStorage;
    private final RaftStateListener raftStateListener;
    private final ServiceName<LogStream> logStreamServiceName;
    private final OnOpenLogStreamListener onOpenLogStreamListener;
    private final ServiceName<Raft> raftServiceName;

    private Injector<ActorScheduler> actorSchedulerInjector = new Injector<>();
    private Injector<BufferingServerTransport> serverTransportInjector = new Injector<>();
    private Injector<ClientTransport> clientTransportInjector = new Injector<>();
    private Raft raft;

    private CompletableActorFuture<Void> raftServiceCloseFuture;
    private CompletableActorFuture<Void> raftServiceOpenFuture;
    private ServiceStartContext startContext;
    private RaftState currentRaftState;

    public RaftService(final RaftConfiguration configuration, final SocketAddress socketAddress, final LogStream logStream,
                       final List<SocketAddress> members, final RaftPersistentStorage persistentStorage,
                       RaftStateListener raftStateListener, OnOpenLogStreamListener onOpenLogStreamListener,
                       ServiceName<Raft> raftServiceName)
    {
        this.configuration = configuration;
        this.socketAddress = socketAddress;
        this.logStream = logStream;
        this.members = members;
        this.persistentStorage = persistentStorage;
        this.raftStateListener = raftStateListener;
        this.logStreamServiceName = logStreamServiceName(logStream.getLogName());
        this.onOpenLogStreamListener = onOpenLogStreamListener;
        this.raftServiceName = raftServiceName;

    }

    @Override
    protected void onActorStarted()
    {
        actor.runOnCompletion(logStream.openAsync(), ((aVoid, throwable) ->
        {
            if (throwable == null)
            {
                final BufferingServerTransport serverTransport = serverTransportInjector.getValue();
                final ClientTransport clientTransport = clientTransportInjector.getValue();

                raft = new Raft(
                                configuration,
                                socketAddress,
                                logStream,
                                serverTransport,
                                clientTransport,
                                persistentStorage,
                                raftStateListener,
                                RaftService.this);

                raft.addMembers(members);
                final ActorScheduler actorScheduler = actorSchedulerInjector.getValue();
                actorScheduler.submitActor(raft);

                raftServiceOpenFuture.complete(null);
            }
            else
            {
                raftServiceOpenFuture.completeExceptionally(throwable);
                Loggers.CLUSTERING_LOGGER.debug("Failed to appendEvent log stream.");
            }
        }));
    }

    @Override
    public void start(final ServiceStartContext startContext)
    {
        raftServiceOpenFuture = new CompletableActorFuture<>();
        actorSchedulerInjector.getValue().submitActor(this);

        this.startContext = startContext;
        this.startContext.async(raftServiceOpenFuture);
    }

    @Override
    protected void onActorClosing()
    {
    }

    @Override
    public void stop(final ServiceStopContext stopContext)
    {
        raftServiceCloseFuture = new CompletableActorFuture<>();

        actor.call(() ->
        {
            actor.runOnCompletion(raft.close(), (v1, t1) ->
            {
                actor.runOnCompletion(logStream.closeAsync(), ((v2, t2) ->
                {
                    if (t1 != null)
                    {
                        raftServiceCloseFuture.completeExceptionally(t1);
                    }
                    else if (t2 != null)
                    {
                        raftServiceCloseFuture.completeExceptionally(t2);
                    }
                    else
                    {
                        raftServiceCloseFuture.complete(null);
                    }
                    actor.close();
                }));
            });
        });
        stopContext.async(raftServiceCloseFuture);
    }

    @Override
    public Raft get()
    {
        return raft;
    }

    public Injector<ActorScheduler> getActorSchedulerInjector()
    {
        return actorSchedulerInjector;
    }

    public Injector<BufferingServerTransport> getServerTransportInjector()
    {
        return serverTransportInjector;
    }

    public Injector<ClientTransport> getClientTransportInjector()
    {
        return clientTransportInjector;
    }

    @Override
    public void onStateChange(int partitionId, DirectBuffer topicName, SocketAddress socketAddress, RaftState raftState)
    {
        actor.call(() ->
        {
            currentRaftState = raftState;

            if (currentRaftState == RaftState.LEADER)
            {
                Loggers.CLUSTERING_LOGGER.debug("Start log stream...topic {}", BufferUtil.bufferAsString(raft.getLogStream().getTopicName()));
                final LogStream logStream = raft.getLogStream();
                final LogStreamService service = new LogStreamService(logStream);

                final ServiceName<LogStream> streamGroup = Protocol.SYSTEM_TOPIC_BUF.equals(logStream.getTopicName()) ?
                    LogStreamServiceNames.SYSTEM_STREAM_GROUP :
                    LogStreamServiceNames.WORKFLOW_STREAM_GROUP;

                final ActorFuture<Void> future =
                    startContext
                        .createService(logStreamServiceName, service)
                        .dependency(ACTOR_SCHEDULER_SERVICE)
                        .dependency(CLUSTER_MANAGER_SERVICE)
                        .dependency(raftServiceName)
                        .group(streamGroup)
                        .install();

                actor.runOnCompletion(future, (v, throwable) ->
                {
                    if (throwable == null)
                    {
                        actor.submit(() ->
                        {
                            onOpenLogStreamListener.onOpenLogStreamService(raft.getLogStream());
                        });
                    }
                    else
                    {
                        LOG.error("Failed to install log stream service '{}'", logStreamServiceName);
                    }

                });
            }
            else if (currentRaftState == RaftState.FOLLOWER &&
                     startContext.hasService(logStreamServiceName))
            {
                startContext.removeService(logStreamServiceName);
            }

        });
    }
}
