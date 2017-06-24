package org.camunda.tngp.broker.it.workflow;

import static org.assertj.core.api.Assertions.*;

import org.camunda.tngp.broker.it.ClientRule;
import org.camunda.tngp.broker.it.EmbeddedBrokerRule;
import org.camunda.tngp.client.WorkflowTopicClient;
import org.camunda.tngp.client.workflow.cmd.DeploymentResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class DeployYamlResourceTest
{
    public EmbeddedBrokerRule brokerRule = new EmbeddedBrokerRule();

    public ClientRule clientRule = new ClientRule();

    @Rule
    public RuleChain ruleChain = RuleChain
        .outerRule(brokerRule)
        .around(clientRule);

    @Test
    public void shouldDeployModelInstance()
    {
        // given
        final WorkflowTopicClient workflowService = clientRule.workflowTopic();

        final DeploymentResult deploymentResult = workflowService.deploy()
            .resourceFromClasspath("org/camunda/tngp/broker/it/workflow/two-tasks.yaml")
            .execute();

        assertThat(deploymentResult.isDeployed()).isTrue();

    }
}
