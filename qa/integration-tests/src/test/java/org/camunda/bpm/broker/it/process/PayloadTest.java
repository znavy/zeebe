package org.camunda.bpm.broker.it.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.agrona.DirectBuffer;
import org.camunda.bpm.broker.it.ClientRule;
import org.camunda.bpm.broker.it.EmbeddedBrokerRule;
import org.camunda.bpm.broker.it.TestUtil;
import org.camunda.tngp.broker.test.util.MsgPackUtil;
import org.camunda.tngp.client.AsyncTasksClient;
import org.camunda.tngp.client.TngpClient;
import org.camunda.tngp.client.WorkflowsClient;
import org.camunda.tngp.client.cmd.BrokerRequestException;
import org.camunda.tngp.client.cmd.WorkflowDefinition;
import org.camunda.tngp.client.task.Payload;
import org.camunda.tngp.client.task.Task;
import org.camunda.tngp.client.task.TaskHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PayloadTest
{

    public EmbeddedBrokerRule brokerRule = new EmbeddedBrokerRule();

    public ClientRule clientRule = new ClientRule();

    @Rule
    public RuleChain ruleChain = RuleChain
        .outerRule(brokerRule)
        .around(clientRule);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    protected ObjectMapper objectMapper;

    @Before
    public void setUp()
    {
        objectMapper = new ObjectMapper(new MessagePackFactory());
    }

    @Test
    public void testPayloadModification() throws InterruptedException
    {
        // given
        final TngpClient client = clientRule.getClient();
        final AsyncTasksClient taskService = client.tasks();
        final WorkflowsClient workflowsClient = client.workflows();

        final WorkflowDefinition workflowDefinition = workflowsClient.deploy()
            .bpmnModelInstance(ProcessModels.TWO_TASKS_PROCESS)
            .execute();

        final DirectBuffer payloadBuffer = MsgPackUtil.encodeMsgPack((p) ->
        {
            p.packMapHeader(1);
            p.packString("key");
            p.packString("val");
        });

        workflowsClient
            .start()
            .workflowDefinitionId(workflowDefinition.getId())
            .payload(payloadBuffer, 0, payloadBuffer.capacity())
            .execute();

        // when
        final PayloadRecordingHandler taskHandler = new PayloadRecordingHandler();

        taskService.newSubscription()
            .handler((t) ->
            {
                taskHandler.handle(t);
                final MyCustomPayload unmarshalledPayload =
                        objectMapper.readValue(t.getPayload().getRaw(), MyCustomPayload.class);
                unmarshalledPayload.setKey("newVal");

                final byte[] marshalledPayload = objectMapper.writeValueAsBytes(unmarshalledPayload);
                t.complete(marshalledPayload);
            })
            .taskType("foo")
            .open();

        taskService.newSubscription()
            .handler(taskHandler)
            .taskType("bar")
            .open();


        // then
        TestUtil.waitUntil(() -> taskHandler.payloads.size() == 2);

        assertThat(taskHandler.payloads).hasSize(2);


        final DirectBuffer expectedUpdatedPayloadBuffer = MsgPackUtil.encodeMsgPack((p) ->
        {
            p.packMapHeader(1);
            p.packString("key");
            p.packString("newVal");
        });

        assertThat(taskHandler.payloads.get(0)).containsExactly(toBytes(payloadBuffer));
        assertThat(taskHandler.payloads.get(1)).containsExactly(toBytes(expectedUpdatedPayloadBuffer));
    }

    @Test
    public void shouldRejectInvalidMsgPack()
    {
     // given
        final TngpClient client = clientRule.getClient();
        final WorkflowsClient workflowsClient = client.workflows();

        final WorkflowDefinition workflowDefinition = workflowsClient.deploy()
            .bpmnModelInstance(ProcessModels.TWO_TASKS_PROCESS)
            .execute();

        // then
        exception.expect(BrokerRequestException.class);
        exception.expectMessage("Invalid msgpack payload. Error at position 0: Unrecognized token format");

        // when
        workflowsClient
            .start()
            .workflowDefinitionId(workflowDefinition.getId())
            .payload(new byte[]{ (byte) 0xc7 }) // ext8 header that is currently not supported by us
            .execute();
    }

    public static final class PayloadRecordingHandler implements TaskHandler
    {

        protected List<byte[]> payloads = new ArrayList<>();

        @Override
        public void handle(Task task)
        {
            final Payload payload = task.getPayload();
            final byte[] bytes = new byte[payload.rawSize()];
            try
            {
                payload.getRaw().read(bytes, 0, bytes.length);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }


            payloads.add(bytes);
        }

    }

    protected static byte[] toBytes(DirectBuffer buffer)
    {
        final byte[] result = new byte[buffer.capacity()];
        buffer.getBytes(0, result);
        return result;
    }

}
