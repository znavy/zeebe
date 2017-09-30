/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.logstreams;

import io.zeebe.broker.logstreams.cfg.LogStreamsCfg;
import io.zeebe.broker.services.Counters;
import io.zeebe.broker.system.ConfigurationManager;
import io.zeebe.servicecontainer.Injector;
import io.zeebe.servicecontainer.Service;
import io.zeebe.servicecontainer.ServiceStartContext;
import io.zeebe.servicecontainer.ServiceStopContext;
import io.zeebe.util.actor.ActorScheduler;

public class LogStreamsManagerService implements Service<LogStreamsManager>
{

    protected final Injector<ActorScheduler> actorSchedulerInjector = new Injector<>();
    protected final Injector<Counters> countersInjector = new Injector<>();

    protected LogStreamsCfg logStreamsCfg;

    protected LogStreamsManager service;

    public LogStreamsManagerService(ConfigurationManager configurationManager)
    {
        logStreamsCfg = configurationManager.readEntry("logs", LogStreamsCfg.class);
    }

    @Override
    public void start(ServiceStartContext serviceContext)
    {
        final Counters counters = countersInjector.getValue();
        service = new LogStreamsManager(logStreamsCfg, actorSchedulerInjector.getValue(), counters.getCountersManager());
    }

    @Override
    public void stop(ServiceStopContext stopContext)
    {
        // nothing to do
    }

    @Override
    public LogStreamsManager get()
    {
        return service;
    }

    public Injector<ActorScheduler> getActorSchedulerInjector()
    {
        return actorSchedulerInjector;
    }

    public Injector<Counters> getCountersInjector()
    {
        return countersInjector;
    }

}
