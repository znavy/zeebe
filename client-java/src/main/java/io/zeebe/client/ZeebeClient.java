/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client;

import java.util.Properties;

import io.zeebe.client.impl.ZeebeClientImpl;
import io.zeebe.client.topology.Topology;

public interface ZeebeClient extends AutoCloseable
{
    /**
     * The name of the default topic
     */
    String DEFAULT_TOPIC_NAME = "default-topic";

    Topology getTopology();

    /**
     * Disconnects the client from the configured brokers.
     */
    void disconnectAll();

    @Override
    void close();

    static ZeebeClient create(Properties properties)
    {
        return new ZeebeClientImpl(properties);
    }

    static ZeebeClient createDefaultClient()
    {
        return create(new Properties());
    }

    TaskClient taskClient();

}
