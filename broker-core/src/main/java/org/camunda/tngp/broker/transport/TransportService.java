package org.camunda.tngp.broker.transport;

import org.camunda.tngp.dispatcher.Dispatcher;
import org.camunda.tngp.servicecontainer.Injector;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceStartContext;
import org.camunda.tngp.servicecontainer.ServiceStopContext;
import org.camunda.tngp.transport.Transport;
import org.camunda.tngp.transport.TransportBuilder;
import org.camunda.tngp.transport.Transports;
import org.camunda.tngp.transport.impl.agent.Conductor;
import org.camunda.tngp.transport.impl.agent.Receiver;
import org.camunda.tngp.transport.impl.agent.Sender;
import org.camunda.tngp.util.newagent.ScheduledTask;
import org.camunda.tngp.util.newagent.TaskScheduler;

public class TransportService implements Service<Transport>
{
    protected final Injector<Dispatcher> sendBufferInjector = new Injector<>();
    protected final Injector<TaskScheduler> taskSchedulerInjector = new Injector<>();

    protected Transport transport;
    protected ScheduledTask scheduledTransportConductor;
    protected ScheduledTask scheduledReceiver;
    protected ScheduledTask scheduledSender;

    @Override
    public void start(ServiceStartContext serviceContext)
    {
        final TransportBuilder transportBuilder = Transports.createTransport(serviceContext.getName());

        transport = transportBuilder
                .sendBuffer(sendBufferInjector.getValue())
                .tasksExternallyManaged()
                .build();

        final Conductor transportConductor = transportBuilder.getTransportConductor();
        final Receiver receiver = transportBuilder.getReceiver();
        final Sender sender = transportBuilder.getSender();

        final TaskScheduler taskScheduler = taskSchedulerInjector.getValue();
        scheduledReceiver = taskScheduler.submitTask(receiver);
        scheduledSender = taskScheduler.submitTask(sender);
        scheduledTransportConductor = taskScheduler.submitTask(transportConductor);
    }

    @Override
    public void stop(ServiceStopContext stopContext)
    {
        scheduledReceiver.remove();
        scheduledSender.remove();
        scheduledTransportConductor.remove();
    }

    @Override
    public Transport get()
    {
        return transport;
    }

    public Injector<TaskScheduler> getTaskSchedulerInjector()
    {
        return taskSchedulerInjector;
    }

    public Injector<Dispatcher> getSendBufferInjector()
    {
        return sendBufferInjector;
    }
}
