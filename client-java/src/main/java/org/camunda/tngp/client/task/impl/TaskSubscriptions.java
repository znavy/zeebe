package org.camunda.tngp.client.task.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskSubscriptions
{
    // TODO: should have a status such that no new subscriptions can be added once closeAll has been called
    // TODO: Idee: Acquistion sollte single writer der subscriptions sein; subscriptions sollen Zustand haben (closing, etc.)
    //   => Acquisition übernimmt das Schließen der Subscription und kann ohne Synchronisation entscheiden,
    //   wann für die Subscription garantiert keine Tasks mehr akquiriert werden


    protected List<TaskSubscriptionImpl> pollableSubscriptions = new CopyOnWriteArrayList<>();
    protected List<TaskSubscriptionImpl> managedExecutionSubscriptions = new CopyOnWriteArrayList<>();

    public void addPollableSubscription(TaskSubscriptionImpl subscription)
    {
        this.pollableSubscriptions.add(subscription);
    }

    public void addManagedExecutionSubscription(TaskSubscriptionImpl subscription)
    {
        this.managedExecutionSubscriptions.add(subscription);
    }

    public void closeAll()
    {
        for (TaskSubscriptionImpl subscription : pollableSubscriptions)
        {
            subscription.close();
        }

        for (TaskSubscriptionImpl subscription : managedExecutionSubscriptions)
        {
            subscription.close();
        }
    }

    public List<TaskSubscriptionImpl> getManagedExecutionSubscriptions()
    {
        return managedExecutionSubscriptions;
    }

    public List<TaskSubscriptionImpl> getPollableSubscriptions()
    {
        return pollableSubscriptions;
    }

    public void removeSubscription(TaskSubscriptionImpl subscription)
    {
        // TODO: not so great performance
        pollableSubscriptions.remove(subscription);
        managedExecutionSubscriptions.remove(subscription);
    }

}
