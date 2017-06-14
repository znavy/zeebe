package org.camunda.tngp.broker.system.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.status.AtomicCounter;
import org.agrona.concurrent.status.CountersManager;
import org.camunda.tngp.broker.services.Counters;
import org.camunda.tngp.broker.system.ConfigurationManager;
import org.camunda.tngp.broker.system.threads.cfg.ThreadingCfg;
import org.camunda.tngp.broker.system.threads.cfg.ThreadingCfg.BrokerIdleStrategy;
import org.camunda.tngp.servicecontainer.Injector;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceStartContext;
import org.camunda.tngp.servicecontainer.ServiceStopContext;
import org.camunda.tngp.util.newagent.TaskRunner;
import org.camunda.tngp.util.newagent.TaskScheduler;

public class TaskSchedulerService implements Service<TaskScheduler>
{
    static int maxThreadCount = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);

    protected final Injector<Counters> countersInjector = new Injector<>();

    protected final int availableThreads;

    protected final List<AtomicCounter> errorCounters = new ArrayList<>();

    protected final BrokerIdleStrategy brokerIdleStrategy;
    protected final int maxIdleTimeMs;

    protected TaskScheduler scheduler;

    public TaskSchedulerService(ConfigurationManager configurationManager)
    {
        final ThreadingCfg cfg = configurationManager.readEntry("threading", ThreadingCfg.class);

        int numberOfThreads = cfg.numberOfThreads;

        if (numberOfThreads > maxThreadCount)
        {
            System.err.println("WARNING: configured thread count (" + numberOfThreads + ") is larger than maxThreadCount " +
                    maxThreadCount + "). Falling back max thread count.");
            numberOfThreads = maxThreadCount;
        }
        else if (numberOfThreads < 1)
        {
            // use max threads by default
            numberOfThreads = maxThreadCount;
        }

        availableThreads = numberOfThreads;
        brokerIdleStrategy = cfg.idleStrategy;
        maxIdleTimeMs = cfg.maxIdleTimeMs;
    }

    @Override
    public void start(ServiceStartContext serviceContext)
    {
        final CountersManager countersManager = countersInjector.getValue().getCountersManager();

        scheduler = new TaskScheduler(availableThreads, countersManager, new DefaultTaskRunnerFactory(brokerIdleStrategy));
    }

    @Override
    public void stop(ServiceStopContext stopContext)
    {
        scheduler.close();
    }

    @Override
    public TaskScheduler get()
    {
        return scheduler;
    }

    // TODO simplify
    class DefaultTaskRunnerFactory implements Supplier<TaskRunner>
    {
        private final BrokerIdleStrategy brokerIdleStrategy;

        DefaultTaskRunnerFactory(BrokerIdleStrategy brokerIdleStrategy)
        {
            this.brokerIdleStrategy = brokerIdleStrategy;
        }

        protected IdleStrategy createIdleStrategy(BrokerIdleStrategy idleStrategy)
        {
            switch (idleStrategy)
            {
                case BUSY_SPIN:
                    return new BusySpinIdleStrategy();
                default:
                    return new BackoffIdleStrategy(1000, 100, 100, TimeUnit.MILLISECONDS.toNanos(maxIdleTimeMs));
            }
        }

        @Override
        public TaskRunner get()
        {
            final IdleStrategy idleStrategy = createIdleStrategy(brokerIdleStrategy);

            return new TaskRunner(10, idleStrategy, (t) -> t.printStackTrace());
        }
    }

    public Injector<Counters> getCountersManagerInjector()
    {
        return countersInjector;
    };

}
