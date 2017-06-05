package org.camunda.tngp.broker.clustering.management.topics;

import org.agrona.DirectBuffer;
import org.camunda.tngp.broker.util.msgpack.UnpackedObject;
import org.camunda.tngp.broker.util.msgpack.property.EnumProperty;
import org.camunda.tngp.broker.util.msgpack.property.IntegerProperty;
import org.camunda.tngp.broker.util.msgpack.property.StringProperty;

public class TopicManagementEvent extends UnpackedObject
{
    private final EnumProperty<TopicManagementEventType> eventTypeProp = new EnumProperty<>("eventType", TopicManagementEventType.class);
    private final StringProperty topicNameProp = new StringProperty("topicName");
    private final IntegerProperty partitionCountProp = new IntegerProperty("partitionCount", 1);
    private final IntegerProperty partitionCreateIdx = new IntegerProperty("partitionCreateIdx", 0);

    public TopicManagementEvent()
    {
        this.declareProperty(eventTypeProp)
            .declareProperty(topicNameProp)
            .declareProperty(partitionCountProp);
    }

    public TopicManagementEventType getEventType()
    {
        return eventTypeProp.getValue();
    }

    public void setEventType(TopicManagementEventType type)
    {
        eventTypeProp.setValue(type);
    }

    public DirectBuffer getTopicName()
    {
        return topicNameProp.getValue();
    }

    public String getTopicNameString()
    {
        final DirectBuffer topicNameBuff = getTopicName();
        return topicNameBuff.getStringWithoutLengthUtf8(0, topicNameBuff.capacity());
    }

    public int getPartitionCount()
    {
        return partitionCountProp.getValue();
    }

    public int getPartitionCreateIdx()
    {
        return partitionCreateIdx.getValue();
    }

    public void setPartitionCreateIdx(int idx)
    {
        partitionCreateIdx.setValue(idx);
    }
}
