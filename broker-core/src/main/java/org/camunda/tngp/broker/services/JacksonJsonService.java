package org.camunda.tngp.broker.services;

import org.camunda.tngp.broker.wf.runtime.data.JacksonJsonDocument;
import org.camunda.tngp.broker.wf.runtime.data.JacksonJsonValidator;
import org.camunda.tngp.broker.wf.runtime.data.JsonDocument;
import org.camunda.tngp.broker.wf.runtime.data.JsonValidator;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceStartContext;
import org.camunda.tngp.servicecontainer.ServiceStopContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

public class JacksonJsonService implements Service<JsonConfiguration>
{

    protected JsonConfiguration configuration;

    @Override
    public void start(ServiceStartContext startContext)
    {
        configuration = buildJsonConfiguration();
    }

    @Override
    public void stop(ServiceStopContext stopContext)
    {
    }

    @Override
    public JsonConfiguration get()
    {
        return configuration;
    }

    public static JsonConfiguration buildJsonConfiguration()
    {
        final JacksonJsonConfiguration configuration = new JacksonJsonConfiguration();
        configuration.objectMapper = new ObjectMapper();
        configuration.jsonPathConfiguration = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .build();

        return configuration;
    }

    public static class JacksonJsonConfiguration implements JsonConfiguration
    {
        protected ObjectMapper objectMapper;
        protected Configuration jsonPathConfiguration;

        public ObjectMapper getObjectMapper()
        {
            return objectMapper;
        }

        public Configuration getJsonPathConfiguration()
        {
            return jsonPathConfiguration;
        }

        @Override
        public JsonDocument buildJsonDocument(int numConcurrentResults)
        {
            return new JacksonJsonDocument(objectMapper, jsonPathConfiguration, numConcurrentResults);
        }

        @Override
        public JsonValidator buildJsonValidator()
        {
            return new JacksonJsonValidator(objectMapper);
        }
    }


}
