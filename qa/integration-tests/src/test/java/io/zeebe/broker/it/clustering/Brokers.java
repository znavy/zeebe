package io.zeebe.broker.it.clustering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zeebe.broker.Broker;
import io.zeebe.transport.SocketAddress;

public class Brokers implements AutoCloseable
{

    public static final String BROKER_1_TOML = "zeebe.cluster.1.cfg.toml";
    public static final SocketAddress BROKER_1_CLIENT_ADDRESS = new SocketAddress("localhost", 51015);

    public static final String BROKER_2_TOML = "zeebe.cluster.2.cfg.toml";
    public static final SocketAddress BROKER_2_CLIENT_ADDRESS = new SocketAddress("localhost", 41015);
    public static final SocketAddress BROKER_2_RAFT_ADDRESS = new SocketAddress("localhost", 41017);

    public static final String BROKER_3_TOML = "zeebe.cluster.3.cfg.toml";
    public static final SocketAddress BROKER_3_CLIENT_ADDRESS = new SocketAddress("localhost", 31015);
    public static final SocketAddress BROKER_3_RAFT_ADDRESS = new SocketAddress("localhost", 31017);

    public static final Logger LOG = LoggerFactory.getLogger(Brokers.class);

    protected final Map<SocketAddress, Broker> brokers = new HashMap<>();

    public void startBroker(final SocketAddress socketAddress, final String configFilePath)
    {
        LOG.info("starting broker {} with config {}", socketAddress, configFilePath);

        try (InputStream config = Brokers.class.getClassLoader().getResourceAsStream(configFilePath))
        {
            assertThat(config).isNotNull();
            brokers.put(socketAddress, new Broker(config));
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Unable to read configuration", e);
        }
    }

    public Set<SocketAddress> getBrokerAddresses()
    {
        return new HashSet<>(brokers.keySet());
    }


    public void close(SocketAddress brokerAddress)
    {
        brokers.get(brokerAddress).close();
    }

    public Broker get(SocketAddress brokerAddress)
    {
        return brokers.get(brokerAddress);
    }

    @Override
    public void close() throws Exception
    {
        for (Broker broker : brokers.values())
        {
            broker.close();
        }
    }
}
