/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.tngp.broker.task;

import org.camunda.tngp.logstreams.log.LogStream;
import org.camunda.tngp.servicecontainer.*;
import org.camunda.tngp.util.newagent.ScheduledTask;
import org.camunda.tngp.util.newagent.TaskScheduler;

public class TaskSubscriptionManagerService implements Service<TaskSubscriptionManager>
{
    protected final Injector<TaskScheduler> taskSchedulerInjector = new Injector<>();

    protected ServiceStartContext serviceContext;

    protected TaskSubscriptionManager service;
    protected ScheduledTask scheduledService;

    protected final ServiceGroupReference<LogStream> logStreamsGroupReference = ServiceGroupReference.<LogStream>create()
        .onAdd((name, stream) -> service.addStream(stream, name))
        .onRemove((name, stream) -> service.removeStream(stream))
        .build();

    @Override
    public void start(ServiceStartContext startContext)
    {
        final TaskScheduler taskScheduler = taskSchedulerInjector.getValue();

        service = new TaskSubscriptionManager(startContext);

        scheduledService = taskScheduler.submitTask(service);
    }

    @Override
    public void stop(ServiceStopContext stopContext)
    {
        scheduledService.remove();
    }

    @Override
    public TaskSubscriptionManager get()
    {
        return service;
    }

    public Injector<TaskScheduler> getTaskSchedulerInjector()
    {
        return taskSchedulerInjector;
    }

    public ServiceGroupReference<LogStream> getLogStreamsGroupReference()
    {
        return logStreamsGroupReference;
    }

}
