package org.camunda.tngp.broker.wf.runtime.data;

import org.camunda.tngp.broker.services.JacksonJsonService;
import org.camunda.tngp.broker.services.JsonConfiguration;
import org.camunda.tngp.broker.system.Component;
import org.camunda.tngp.broker.system.SystemContext;
import org.camunda.tngp.servicecontainer.ServiceContainer;
import org.camunda.tngp.servicecontainer.ServiceName;

public class JsonComponent implements Component
{

    public static final ServiceName<JsonConfiguration> JSON_SERVICE_NAME =
            ServiceName.newServiceName("json.jackson", JsonConfiguration.class);

    @Override
    public void init(SystemContext context)
    {
        final ServiceContainer serviceContainer = context.getServiceContainer();

        final JacksonJsonService jsonService = new JacksonJsonService();
        serviceContainer
            .createService(JSON_SERVICE_NAME, jsonService)
            .install();
    }

}
