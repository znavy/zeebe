package org.camunda.tngp.broker.clustering.management.topics;

/**
 * Defines a topic
 *
 */
public class TopicDefinition
{
    private String name;

    private int partitionCount;

    // TODO
    // private int replicationFactor;

    public TopicDefinition()
    {
    }

    public TopicDefinition(String name, int partitionCount)
    {
        this.name = name;
        this.partitionCount = partitionCount;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getPartitionCount()
    {
        return partitionCount;
    }

    public void setPartitionCount(int partitionCount)
    {
        this.partitionCount = partitionCount;
    }
}
