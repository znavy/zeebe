package org.camunda.tngp.broker.logstreams;

import org.camunda.tngp.broker.logstreams.cfg.LogStreamsCfg;
import org.camunda.tngp.broker.system.ConfigurationManager;
import org.camunda.tngp.broker.system.threads.AgentRunnerServices;
import org.camunda.tngp.servicecontainer.Injector;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceStartContext;
import org.camunda.tngp.servicecontainer.ServiceStopContext;

public class LogStreamsFactoryService implements Service<LogStreamsFactory>
{
    protected final Injector<AgentRunnerServices> agentRunnerInjector = new Injector<>();

    protected ServiceStartContext serviceContext;
    protected LogStreamsCfg logStreamsCfg;

    protected LogStreamsFactory service;

    public LogStreamsFactoryService(ConfigurationManager configurationManager)
    {
        logStreamsCfg = configurationManager.readEntry("logs", LogStreamsCfg.class);
    }

    @Override
    public void start(ServiceStartContext serviceContext)
    {
        this.serviceContext = serviceContext;

        serviceContext.run(() ->
        {
            service = new LogStreamsFactory(logStreamsCfg, agentRunnerInjector.getValue());
        });
    }

    @Override
    public void stop(ServiceStopContext stopContext)
    {
        // nothing to do
    }

    @Override
    public LogStreamsFactory get()
    {
        return service;
    }

    public Injector<AgentRunnerServices> getAgentRunnerInjector()
    {
        return agentRunnerInjector;
    }

}
