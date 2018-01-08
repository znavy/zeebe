package io.zeebe.broker.clustering.gossip.service;

import io.zeebe.gossip.GossipConfiguration;

/**
 *
 */
public class ZbGossipConfig extends GossipConfiguration
{
    public String[] initialContactPoints = new String[0];
}
