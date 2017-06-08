package org.camunda.tngp.broker.it;

import java.io.InputStream;
import java.util.function.Supplier;

import org.camunda.tngp.broker.Broker;
import org.junit.rules.ExternalResource;

public class EmbeddedBrokerRule2 extends ExternalResource
{
    protected static Broker broker;

    protected Supplier<InputStream> configSupplier;

    public EmbeddedBrokerRule2()
    {
        this(() -> null);
    }

    public EmbeddedBrokerRule2(Supplier<InputStream> configSupplier)
    {
        this.configSupplier = configSupplier;
    }

    @Override
    protected void before() throws Throwable
    {
        if (broker == null)
        {
            startBroker();
        }
    }

    public void restartBroker()
    {
        stopBroker();
        startBroker();
    }

    protected void stopBroker()
    {
        broker.close();
        broker = null;
    }

    protected void startBroker()
    {
        broker = new Broker(configSupplier.get());
    }

}
