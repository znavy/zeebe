package org.camunda.tngp.broker.clustering.gossip.service;

import java.io.File;

import org.camunda.tngp.broker.clustering.gossip.Gossip;
import org.camunda.tngp.broker.clustering.gossip.GossipContext;
import org.camunda.tngp.broker.clustering.gossip.config.GossipConfiguration;
import org.camunda.tngp.broker.system.SystemContext;
import org.camunda.tngp.servicecontainer.Injector;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceStartContext;
import org.camunda.tngp.servicecontainer.ServiceStopContext;
import org.camunda.tngp.util.LangUtil;
import org.camunda.tngp.util.newagent.ScheduledTask;
import org.camunda.tngp.util.newagent.TaskScheduler;

public class GossipService implements Service<Gossip>
{
    private final Injector<TaskScheduler> taskSchedulerInjector = new Injector<>();
    private final Injector<GossipContext> gossipContextInjector = new Injector<>();

    private Gossip gossip;
    private SystemContext systemContext;
    private GossipContext gossipContext;
    private ScheduledTask scheduledGossip;

    public GossipService(SystemContext context)
    {
        this.systemContext = context;
    }

    @Override
    public void start(ServiceStartContext startContext)
    {
        final TaskScheduler taskScheduler = taskSchedulerInjector.getValue();
        this.gossipContext = gossipContextInjector.getValue();
        startContext.run(() ->
        {
            //create a gossip folder
            final GossipConfiguration configuration = gossipContext.getConfig();
            final File f = new File(configuration.directory + Gossip.GOSSIP_FILE_NAME);
            if (!f.exists())
            {
                try
                {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                }
                catch (Exception e)
                {
                    LangUtil.rethrowUnchecked(e);
                }
            }

            this.gossip = new Gossip(gossipContext);
            gossip.open();
            scheduledGossip = taskScheduler.submitTask(gossip);
        });
    }

    @Override
    public void stop(ServiceStopContext stopContext)
    {
        scheduledGossip.remove();
    }

    @Override
    public Gossip get()
    {
        return gossip;
    }

    public Injector<GossipContext> getGossipContextInjector()
    {
        return gossipContextInjector;
    }

    public Injector<TaskScheduler> getTaskSchedulerInjector()
    {
        return taskSchedulerInjector;
    }

}
