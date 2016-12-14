package org.camunda.bpm.broker.it.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.broker.it.ClientRule;
import org.camunda.bpm.broker.it.EmbeddedBrokerRule;
import org.camunda.bpm.broker.it.TestUtil;
import org.camunda.tngp.client.AsyncTasksClient;
import org.camunda.tngp.client.TngpClient;
import org.camunda.tngp.client.WorkflowsClient;
import org.camunda.tngp.client.cmd.WorkflowDefinition;
import org.camunda.tngp.client.task.Payload;
import org.camunda.tngp.client.task.Task;
import org.camunda.tngp.client.task.TaskHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;

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
        objectMapper = new ObjectMapper();
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

        final byte[] payload = "{\"key\":\"val\"}".getBytes(StandardCharsets.UTF_8);

        workflowsClient
            .start()
            .workflowDefinitionId(workflowDefinition.getId())
            .payload(payload)
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


        final byte[] expectedUpdatedPayload = "{\"key\":\"newVal\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(taskHandler.payloads.get(0)).containsExactly(payload);
        assertThat(taskHandler.payloads.get(1)).containsExactly(expectedUpdatedPayload);
    }

    @Test
    public void shouldRejectInvalidJson()
    {
        // given
        final TngpClient client = clientRule.getClient();
        final WorkflowsClient workflowsClient = client.workflows();

        final WorkflowDefinition workflowDefinition = workflowsClient.deploy()
            .bpmnModelInstance(ProcessModels.TWO_TASKS_PROCESS)
            .execute();

        // then
        exception.expect(RuntimeException.class);
        exception.expectMessage("Payload is not valid JSON");

        // when
        workflowsClient
            .start()
            .workflowDefinitionId(workflowDefinition.getId())
            .payload("{invalid json")
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

}
