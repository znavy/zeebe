package org.camunda.tngp.broker.transport.worker;

import org.camunda.tngp.servicecontainer.Injector;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceStartContext;
import org.camunda.tngp.servicecontainer.ServiceStopContext;
import org.camunda.tngp.transport.requestresponse.server.AsyncRequestWorker;
import org.camunda.tngp.transport.requestresponse.server.AsyncRequestWorkerContext;
import org.camunda.tngp.util.newagent.ScheduledTask;
import org.camunda.tngp.util.newagent.TaskScheduler;

public class AsyncRequestWorkerService implements Service<AsyncRequestWorker>
{
    protected final Injector<TaskScheduler> taskSchedulerInjector = new Injector<>();
    protected final Injector<AsyncRequestWorkerContext> workerContextInjector = new Injector<>();

    protected ScheduledTask scheduledWorker;

    @Override
    public void start(ServiceStartContext serviceContext)
    {
        final AsyncRequestWorkerContext workerContext = workerContextInjector.getValue();
        final TaskScheduler taskScheduler = taskSchedulerInjector.getValue();

        final AsyncRequestWorker worker = createWorker(serviceContext.getName(), workerContext);

        scheduledWorker = taskScheduler.submitTask(worker);
    }

    protected AsyncRequestWorker createWorker(String name, final AsyncRequestWorkerContext workerContext)
    {
        return new AsyncRequestWorker(name, workerContext);
    }

    @Override
    public void stop(ServiceStopContext stopContext)
    {
        scheduledWorker.remove();
    }

    @Override
    public AsyncRequestWorker get()
    {
        return null;
    }

    public Injector<TaskScheduler> getTaskSchedulerInjector()
    {
        return taskSchedulerInjector;
    }

    public Injector<AsyncRequestWorkerContext> getWorkerContextInjector()
    {
        return workerContextInjector;
    }
}