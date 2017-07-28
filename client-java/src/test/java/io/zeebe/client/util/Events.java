package io.zeebe.client.util;

import org.assertj.core.util.Maps;

import io.zeebe.client.event.impl.TaskEventImpl;
import io.zeebe.client.workflow.impl.WorkflowInstanceEventImpl;
import io.zeebe.test.broker.protocol.clientapi.ClientApiRule;
import io.zeebe.util.time.ClockUtil;

public class Events
{

    public static TaskEventImpl exampleTask()
    {
        final TaskEventImpl baseEvent = new TaskEventImpl("CREATED");
        baseEvent.setHeaders(Maps.newHashMap("foo", "bar"));
        baseEvent.setKey(79);
        baseEvent.setLockOwner("foo");
        baseEvent.setLockTime(ClockUtil.getCurrentTimeInMillis());
        baseEvent.setPartitionId(ClientApiRule.DEFAULT_PARTITION_ID);
        baseEvent.setPayload("{\"key\":\"val\"}");
        baseEvent.setRetries(123);
        baseEvent.setTopicName(ClientApiRule.DEFAULT_TOPIC_NAME);
        baseEvent.setType("taskTypeFoo");

        return baseEvent;
    }

    public static WorkflowInstanceEventImpl exampleWorfklowInstance()
    {
        final WorkflowInstanceEventImpl baseEvent = new WorkflowInstanceEventImpl("CREATED");
        baseEvent.setActivityId("some_activity");
        baseEvent.setBpmnProcessId("some_proceess");
        baseEvent.setKey(89);
        baseEvent.setPayloadAsJson("{\"key\":\"val\"}");
        baseEvent.setPartitionId(ClientApiRule.DEFAULT_PARTITION_ID);
        baseEvent.setTopicName(ClientApiRule.DEFAULT_TOPIC_NAME);
        baseEvent.setVersion(123);
        baseEvent.setWorkflowInstanceKey(456L);
        baseEvent.setWorkflowKey(789L);

        return baseEvent;
    }
}
