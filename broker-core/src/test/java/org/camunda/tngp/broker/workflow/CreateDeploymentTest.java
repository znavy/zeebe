package org.camunda.tngp.broker.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.broker.workflow.data.WorkflowInstanceEvent.*;
import static org.camunda.tngp.logstreams.log.LogStream.DEFAULT_PARTITION_ID;
import static org.camunda.tngp.logstreams.log.LogStream.DEFAULT_TOPIC_NAME;

import java.io.*;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.impl.util.IoUtil;
import org.camunda.tngp.broker.test.EmbeddedBrokerRule;
import org.camunda.tngp.protocol.clientapi.EventType;
import org.camunda.tngp.test.broker.protocol.clientapi.ClientApiRule;
import org.camunda.tngp.test.broker.protocol.clientapi.ExecuteCommandResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class CreateDeploymentTest
{
    public EmbeddedBrokerRule brokerRule = new EmbeddedBrokerRule();

    public ClientApiRule apiRule = new ClientApiRule();

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(brokerRule).around(apiRule);

    @Test
    public void shouldCreateDeployment()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
            .startEvent()
            .endEvent()
            .done();

        // when
        final ExecuteCommandResponse resp = apiRule.createCmdRequest()
                .topicName(DEFAULT_TOPIC_NAME)
                .partitionId(0)
                .eventType(EventType.DEPLOYMENT_EVENT)
                .command()
                    .put(PROP_EVENT_TYPE, "CREATE_DEPLOYMENT")
                    .put("bpmnXml", Bpmn.convertToString(modelInstance))
                .done()
                .sendAndAwait();

        // then
        assertThat(resp.key()).isGreaterThanOrEqualTo(0L);
        assertThat(resp.getTopicName()).isEqualTo(DEFAULT_TOPIC_NAME);
        assertThat(resp.partitionId()).isEqualTo(DEFAULT_PARTITION_ID);
        assertThat(resp.getEvent()).containsEntry(PROP_EVENT_TYPE, "DEPLOYMENT_CREATED");
    }


    @Test
    public void shouldCreateYamlDeployment() throws IOException
    {
        // given
        final String yaml = getStringFromInputStream(CreateDeploymentTest.class.getResourceAsStream("two-tasks.yaml"), false);

        // when
        final ExecuteCommandResponse resp = apiRule.createCmdRequest()
                .topicName(DEFAULT_TOPIC_NAME)
                .partitionId(0)
                .eventType(EventType.DEPLOYMENT_EVENT)
                .command()
                    .put(PROP_EVENT_TYPE, "CREATE_DEPLOYMENT")
                    .put("bpmnXml", yaml)
                .done()
                .sendAndAwait();

        // then
        assertThat(resp.key()).isGreaterThanOrEqualTo(0L);
        assertThat(resp.getTopicName()).isEqualTo(DEFAULT_TOPIC_NAME);
        assertThat(resp.partitionId()).isEqualTo(DEFAULT_PARTITION_ID);
        assertThat(resp.getEvent()).containsEntry(PROP_EVENT_TYPE, "DEPLOYMENT_CREATED");
    }


    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnDeployedWorkflowDefinitions()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
            .startEvent()
            .endEvent()
            .done();

        // when
        ExecuteCommandResponse resp = apiRule.createCmdRequest()
            .topicName(DEFAULT_TOPIC_NAME)
            .partitionId(0)
            .eventType(EventType.DEPLOYMENT_EVENT)
            .command()
                .put(PROP_EVENT_TYPE, "CREATE_DEPLOYMENT")
                .put("bpmnXml", Bpmn.convertToString(modelInstance))
            .done()
            .sendAndAwait();

        // then
        List<Map<String, Object>> deployedWorkflows = (List<Map<String, Object>>) resp.getEvent().get("deployedWorkflows");
        assertThat(deployedWorkflows).hasSize(1);
        assertThat(deployedWorkflows.get(0)).containsEntry(PROP_WORKFLOW_BPMN_PROCESS_ID, "process");
        assertThat(deployedWorkflows.get(0)).containsEntry(PROP_WORKFLOW_VERSION, 1);

        // when deploy the workflow definition a second time
        resp = apiRule.createCmdRequest()
                .topicName(DEFAULT_TOPIC_NAME)
                .partitionId(0)
                .eventType(EventType.DEPLOYMENT_EVENT)
                .command()
                    .put(PROP_EVENT_TYPE, "CREATE_DEPLOYMENT")
                    .put("bpmnXml", Bpmn.convertToString(modelInstance))
                .done()
                .sendAndAwait();

        // then the workflow definition version is increased
        deployedWorkflows = (List<Map<String, Object>>) resp.getEvent().get("deployedWorkflows");
        assertThat(deployedWorkflows).hasSize(1);
        assertThat(deployedWorkflows.get(0)).containsEntry(PROP_WORKFLOW_BPMN_PROCESS_ID, "process");
        assertThat(deployedWorkflows.get(0)).containsEntry(PROP_WORKFLOW_VERSION, 2);
    }

    @Test
    public void shouldRejectDeploymentIfNotValid()
    {
        // given
        final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process").done();

        // when
        final ExecuteCommandResponse resp = apiRule.createCmdRequest()
                .topicName(DEFAULT_TOPIC_NAME)
                .partitionId(0)
                .eventType(EventType.DEPLOYMENT_EVENT)
                .command()
                    .put(PROP_EVENT_TYPE, "CREATE_DEPLOYMENT")
                    .put("bpmnXml", Bpmn.convertToString(modelInstance))
                .done()
                .sendAndAwait();

        // then
        assertThat(resp.key()).isGreaterThanOrEqualTo(0L);
        assertThat(resp.getTopicName()).isEqualTo(DEFAULT_TOPIC_NAME);
        assertThat(resp.partitionId()).isEqualTo(DEFAULT_PARTITION_ID);
        assertThat(resp.getEvent()).containsEntry(PROP_EVENT_TYPE, "DEPLOYMENT_REJECTED");
        assertThat((String) resp.getEvent().get("errorMessage")).contains("The process must contain at least one none start event.");
    }

    @Test
    public void shouldRejectDeploymentIfNotParsable()
    {
        // when
        final ExecuteCommandResponse resp = apiRule.createCmdRequest()
                .topicName(DEFAULT_TOPIC_NAME)
                .partitionId(0)
                .eventType(EventType.DEPLOYMENT_EVENT)
                .command()
                    .put(PROP_EVENT_TYPE, "CREATE_DEPLOYMENT")
                    .put("bpmnXml", "not a workflow")
                .done()
                .sendAndAwait();

        // then
        assertThat(resp.key()).isGreaterThanOrEqualTo(0L);
        assertThat(resp.getTopicName()).isEqualTo(DEFAULT_TOPIC_NAME);
        assertThat(resp.partitionId()).isEqualTo(DEFAULT_PARTITION_ID);
        assertThat(resp.getEvent()).containsEntry(PROP_EVENT_TYPE, "DEPLOYMENT_REJECTED");
        assertThat((String) resp.getEvent().get("errorMessage")).contains("Failed to deploy BPMN model");
    }

    private static String getStringFromInputStream(InputStream inputStream, boolean trim) throws IOException
    {
        BufferedReader bufferedReader = null;
        final StringBuilder stringBuilder = new StringBuilder();
        try
        {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                if (trim)
                {
                    stringBuilder.append(line.trim());
                }
                else
                {
                    stringBuilder.append(line).append("\n");
                }
            }
        }
        finally
        {
            IoUtil.closeSilently(bufferedReader);
        }

        return stringBuilder.toString();
    }
}
