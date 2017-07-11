package io.zeebe.client.clustering;

import io.zeebe.client.clustering.impl.TopologyResponse;
import io.zeebe.client.cmd.ClientCommand;

public interface RequestTopologyCmd extends ClientCommand<TopologyResponse>
{
}
