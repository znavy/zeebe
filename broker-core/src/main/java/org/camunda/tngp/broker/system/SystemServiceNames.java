package org.camunda.tngp.broker.system;

import org.camunda.tngp.broker.services.Counters;
import org.camunda.tngp.broker.system.executor.ScheduledExecutor;
import org.camunda.tngp.servicecontainer.ServiceName;
import org.camunda.tngp.util.newagent.TaskScheduler;

public class SystemServiceNames
{
    public static final ServiceName<TaskScheduler> TASK_SCHEDULER_SERVICE = ServiceName.newServiceName("broker.task.scheduler", TaskScheduler.class);

    public static final ServiceName<Counters> COUNTERS_MANAGER_SERVICE = ServiceName.newServiceName("broker.countersManager", Counters.class);

    public static final ServiceName<ScheduledExecutor> EXECUTOR_SERVICE = ServiceName.newServiceName("broker.executor", ScheduledExecutor.class);
}
