package org.camunda.bpm.broker.it.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.broker.test.util.bpmn.TngpModelInstance.wrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.camunda.bpm.broker.it.ClientRule;
import org.camunda.bpm.broker.it.EmbeddedBrokerRule;
import org.camunda.bpm.broker.it.TestUtil;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.tngp.client.TngpClient;
import org.camunda.tngp.client.WorkflowsClient;
import org.camunda.tngp.client.cmd.WorkflowDefinition;
import org.camunda.tngp.client.task.Task;
import org.camunda.tngp.client.task.TaskHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class ExclusiveGatewayTest
{
    public static final String TASK_TYPE1 = "foo";
    public static final String TASK_TYPE2 = "bar";

    public static final BpmnModelInstance EXCLUSIVE_GATEWAY_PROCESS = wrap(Bpmn.createExecutableProcess("anId")
            .startEvent()
            .exclusiveGateway("exclusiveGateway")
            .sequenceFlowId("flow1")
            .serviceTask("serviceTask1")
            .endEvent("endEvent1")
            .moveToLastGateway()
            .sequenceFlowId("flow2")
            .serviceTask("serviceTask2")
            .endEvent("endEvent2")
            .done())
        .taskAttributes("serviceTask1", TASK_TYPE1, 0)
        .taskAttributes("serviceTask2", TASK_TYPE2, 0);

    public EmbeddedBrokerRule brokerRule = new EmbeddedBrokerRule();

    public ClientRule clientRule = new ClientRule();

    @Rule
    public RuleChain ruleChain = RuleChain
        .outerRule(brokerRule)
        .around(clientRule);

    @Test
    public void shouldExecuteExclusiveGateway()
    {
        // given
        final TngpClient client = clientRule.getClient();
        final WorkflowsClient workflowService = client.workflows();

        final WorkflowDefinition workflow = clientRule.deployProcess(
             wrap(EXCLUSIVE_GATEWAY_PROCESS)
                .conditionExpression("flow1", "$.key", "EQUAL", "1")
                .conditionExpression("flow2", "$.key", "NOT_EQUAL", "1"));

        final RecordingTaskHandler taskHandler = new RecordingTaskHandler();

        subscribeToTasks(TASK_TYPE1, taskHandler);
        subscribeToTasks(TASK_TYPE2, taskHandler);

        // when
        workflowService
            .start()
            .workflowDefinitionId(workflow.getId())
            .payload("{\"key\":1}")
            .execute();

        // then
        TestUtil.waitUntil(() -> !taskHandler.handledTasks.isEmpty());

        assertThat(taskHandler.handledTasks).hasSize(1);

        final Task task = taskHandler.handledTasks.get(0);
        assertThat(task.getType()).isEqualTo(TASK_TYPE1);
    }

    @Test
    public void testTwoExpressionArguments()
    {
        // given
        final TngpClient client = clientRule.getClient();
        final WorkflowsClient workflowService = client.workflows();

        final WorkflowDefinition workflow = clientRule.deployProcess(
             wrap(EXCLUSIVE_GATEWAY_PROCESS)
                .conditionExpression("flow1", "$.price", "GREATER_THAN", "$.maximum")
                .conditionExpression("flow2", "$.price", "LOWER_THAN_OR_EQUAL", "$.maximum"));

        final RecordingTaskHandler taskHandler = new RecordingTaskHandler();

        subscribeToTasks(TASK_TYPE1, taskHandler);
        subscribeToTasks(TASK_TYPE2, taskHandler);

        // when
        workflowService
            .start()
            .workflowDefinitionId(workflow.getId())
            .payload("{\"price\":2000, \"maximum\":10000}")
            .execute();

        // then
        TestUtil.waitUntil(() -> !taskHandler.handledTasks.isEmpty());

        assertThat(taskHandler.handledTasks).hasSize(1);

        final Task task = taskHandler.handledTasks.get(0);
        assertThat(task.getType()).isEqualTo(TASK_TYPE2);
    }

    @Test
    public void testDefaultFlow()
    {
        // given
        final TngpClient client = clientRule.getClient();
        final WorkflowsClient workflowService = client.workflows();

        final WorkflowDefinition workflow = clientRule.deployProcess(
             wrap(EXCLUSIVE_GATEWAY_PROCESS)
                 .conditionExpression("flow1", "true", "EQUAL", "false")
                 .defaultFlow("flow2"));

        final RecordingTaskHandler taskHandler = new RecordingTaskHandler();

        subscribeToTasks(TASK_TYPE1, taskHandler);
        subscribeToTasks(TASK_TYPE2, taskHandler);

        // when
        workflowService
            .start()
            .workflowDefinitionId(workflow.getId())
            .payload("{\"price\":2000, \"maximum\":10000}")
            .execute();

        // then
        TestUtil.waitUntil(() -> !taskHandler.handledTasks.isEmpty());

        assertThat(taskHandler.handledTasks).hasSize(1);

        final Task task = taskHandler.handledTasks.get(0);
        assertThat(task.getType()).isEqualTo(TASK_TYPE2);
    }

    protected void subscribeToTasks(String taskType, TaskHandler taskHandler)
    {
        clientRule.getClient().tasks().newSubscription()
            .taskQueueId(0)
            .handler(taskHandler)
            .lockTime(10000L)
            .taskType(taskType)
            .open();
    }


    public static class RecordingTaskHandler implements TaskHandler
    {
        protected List<Task> handledTasks = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void handle(Task task)
        {
            handledTasks.add(task);
            task.complete();
        }

        public List<Task> getHandledTasks()
        {
            return handledTasks;
        }

        public void clear()
        {
            handledTasks.clear();
        }
    }
}
