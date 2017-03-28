package org.camunda.tngp.broker.protocol.clientapi;

import static org.camunda.tngp.broker.taskqueue.TaskQueueServiceNames.taskQueueInstanceStreamProcessorServiceName;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.camunda.tngp.broker.Broker;
import org.camunda.tngp.broker.logstreams.LogStreamServiceNames;
import org.camunda.tngp.broker.transport.TransportServiceNames;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceContainer;
import org.camunda.tngp.servicecontainer.ServiceName;
import org.camunda.tngp.servicecontainer.ServiceStartContext;
import org.camunda.tngp.servicecontainer.ServiceStopContext;
import org.junit.rules.ExternalResource;

public class EmbeddedBrokerRule extends ExternalResource
{
    protected Broker broker;

    protected Supplier<InputStream> configSupplier;

    public EmbeddedBrokerRule()
    {
        this(() -> null);
    }

    public EmbeddedBrokerRule(Supplier<InputStream> configSupplier)
    {
        this.configSupplier = configSupplier;
    }

    public EmbeddedBrokerRule(String configFileClasspathLocation)
    {
        this(() -> EmbeddedBrokerRule.class.getClassLoader().getResourceAsStream(configFileClasspathLocation));
    }

    @Override
    protected void before() throws Throwable
    {
        startBroker();
    }

    @Override
    protected void after()
    {
        stopBroker();
    }

    public void restartBroker()
    {
        stopBroker();
        startBroker();
    }

    protected void stopBroker()
    {
        broker.close();
    }

    protected void startBroker()
    {
        broker = new Broker(configSupplier.get());

        final ServiceContainer serviceContainer = broker.getBrokerContext().getServiceContainer();

        try
        {
            // Hack: block until default task queue log has been installed
            // How to make it better: https://github.com/camunda-tngp/camunda-tngp/issues/196
            serviceContainer.createService(TestService.NAME, new TestService())
                .dependency(LogStreamServiceNames.logStreamServiceName("default-task-queue-log"))
                .dependency(TransportServiceNames.serverSocketBindingReceiveBufferName(TransportServiceNames.CLIENT_API_SOCKET_BINDING_NAME))
                .dependency(taskQueueInstanceStreamProcessorServiceName("default-task-queue-log"))
                .install()
                .get(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e)
        {
            stopBroker();
            throw new RuntimeException("Default task queue log not installed into the container withing 5 seconds.");
        }
    }

    static class TestService implements Service<TestService>
    {

        static final ServiceName<TestService> NAME = ServiceName.newServiceName("testService", TestService.class);

        @Override
        public void start(ServiceStartContext startContext)
        {

        }

        @Override
        public void stop(ServiceStopContext stopContext)
        {

        }

        @Override
        public TestService get()
        {
            return this;
        }

    }

}