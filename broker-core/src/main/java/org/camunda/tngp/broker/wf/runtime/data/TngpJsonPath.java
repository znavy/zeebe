package org.camunda.tngp.broker.wf.runtime.data;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

public class TngpJsonPath
{
    // TODO: should not be in a static field but be initialized in the service lifecycle

    protected static Configuration configuration;

    public static Configuration getConfiguration()
    {
        if (configuration == null)
        {
            synchronized (TngpJsonPath.class)
            {
                if (configuration == null)
                {
                    configuration = buildDefaultConfiguration();
                }
            }
        }

        return configuration;
    }

    protected static Configuration buildDefaultConfiguration()
    {
        return Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .build();
    }
}
