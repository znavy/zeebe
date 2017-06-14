package org.camunda.tngp.broker.system;

import static org.camunda.tngp.broker.system.SystemServiceNames.COUNTERS_MANAGER_SERVICE;
import static org.camunda.tngp.broker.system.SystemServiceNames.EXECUTOR_SERVICE;
import static org.camunda.tngp.broker.system.SystemServiceNames.TASK_SCHEDULER_SERVICE;

import org.camunda.tngp.broker.services.CountersManagerService;
import org.camunda.tngp.broker.system.executor.ScheduledExecutorService;
import org.camunda.tngp.broker.system.threads.TaskSchedulerService;
import org.camunda.tngp.servicecontainer.ServiceContainer;

public class SystemComponent implements Component
{

    @Override
    public void init(SystemContext context)
    {
        final ServiceContainer serviceContainer = context.getServiceContainer();

        final CountersManagerService countersManagerService = new CountersManagerService(context.getConfigurationManager());
        serviceContainer.createService(COUNTERS_MANAGER_SERVICE, countersManagerService)
            .install();

        final TaskSchedulerService agentRunnerService = new TaskSchedulerService(context.getConfigurationManager());
        serviceContainer.createService(TASK_SCHEDULER_SERVICE, agentRunnerService)
            .dependency(COUNTERS_MANAGER_SERVICE, agentRunnerService.getCountersManagerInjector())
            .install();

        final ScheduledExecutorService executorService = new ScheduledExecutorService();
        serviceContainer.createService(EXECUTOR_SERVICE, executorService)
            .dependency(TASK_SCHEDULER_SERVICE, executorService.getTaskSchedulerInjector())
            .install();
    }

}
