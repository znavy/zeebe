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
package io.zeebe.client.impl.workflow;

import static io.zeebe.util.EnsureUtil.*;

import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.client.ClientCommandRejectedException;
import io.zeebe.client.impl.Partition;
import io.zeebe.client.impl.cmd.AbstractExecuteCmdImpl;
import io.zeebe.client.impl.cmd.ClientCommandManager;
import io.zeebe.client.impl.data.MsgPackConverter;
import io.zeebe.client.impl.event.TopicClientImpl;
import io.zeebe.client.workflow.cmd.UpdateWorkflowPayloadCmd;
import io.zeebe.protocol.clientapi.EventType;

public class UpdateWorkflowPayloadCmdImpl extends AbstractExecuteCmdImpl<WorkflowInstanceEvent, Void> implements UpdateWorkflowPayloadCmd
{
    private static final String ERROR_MESSAGE = "Failed to update payload of the workflow instance with key '%d'.";

    private final WorkflowInstanceEvent workflowInstanceEvent = new WorkflowInstanceEvent();
    private final MsgPackConverter msgPackConverter;

    private long activityInstanceKey;

    public UpdateWorkflowPayloadCmdImpl(final ClientCommandManager commandManager, final ObjectMapper objectMapper, MsgPackConverter msgPackConverter, final Partition topic)
    {
        super(commandManager, objectMapper, topic, WorkflowInstanceEvent.class, EventType.WORKFLOW_EVENT);
        this.msgPackConverter = msgPackConverter;
    }

    public UpdateWorkflowPayloadCmdImpl(TopicClientImpl topicClientImpl)
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public UpdateWorkflowPayloadCmd activityInstanceKey(final long activityInstanceKey)
    {
        this.activityInstanceKey = activityInstanceKey;
        return this;
    }

    @Override
    public UpdateWorkflowPayloadCmd workflowInstanceKey(final long workflowInstanceKey)
    {
        this.workflowInstanceEvent.setWorkflowInstanceKey(workflowInstanceKey);
        return this;
    }

    @Override
    public UpdateWorkflowPayloadCmd payload(final InputStream payload)
    {
        this.workflowInstanceEvent.setPayload(msgPackConverter.convertToMsgPack(payload));
        return this;
    }

    @Override
    public UpdateWorkflowPayloadCmd payload(final String payload)
    {
        this.workflowInstanceEvent.setPayload(msgPackConverter.convertToMsgPack(payload));
        return this;
    }

    @Override
    protected Object writeCommand()
    {
        workflowInstanceEvent.setEventType(WorkflowInstanceEventType.UPDATE_PAYLOAD);

        return workflowInstanceEvent;
    }

    @Override
    protected long getKey()
    {
        return activityInstanceKey;
    }

    @Override
    protected void reset()
    {
        activityInstanceKey = -1L;
        workflowInstanceEvent.reset();
    }

    @Override
    protected Void getResponseValue(final long key, final WorkflowInstanceEvent event)
    {
        if (event.getEventType() == WorkflowInstanceEventType.UPDATE_PAYLOAD_REJECTED)
        {
            throw new ClientCommandRejectedException(String.format(ERROR_MESSAGE, event.getWorkflowInstanceKey()));
        }
        return null;
    }

    @Override
    public void validate()
    {
        super.validate();
        ensureGreaterThan("activity instance key", activityInstanceKey, 0);
        ensureGreaterThan("workflow instance key", workflowInstanceEvent.getWorkflowInstanceKey(), 0);
        ensureNotNull("payload", workflowInstanceEvent.getPayload());
    }

}
