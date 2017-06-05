package org.camunda.tngp.broker.clustering.management.topics;

import org.camunda.tngp.logstreams.log.LogStream;

/**
 * Defines the built-in system topics
 *
 */
public class SystemTopics
{
    /** TODO: remove me */
    public static final TopicDefinition DEFAULT_TOPIC = new TopicDefinition(LogStream.DEFAULT_TOPIC_NAME, 1);

    /**
     * Holds data related to the management of topics (creating topics, removing topics, etc...)
     */
    public static final TopicDefinition ZB_TOPICS_MANAGEMENT = new TopicDefinition("zb-topics-management", 1);

}
