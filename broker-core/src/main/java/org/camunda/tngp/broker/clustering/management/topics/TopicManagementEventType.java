package org.camunda.tngp.broker.clustering.management.topics;

public enum TopicManagementEventType
{
    CREATE,
    CREATING,
    CREATED,
    FAILING,
    FAILED,
    CREATE_REJECTED,

    DELETE,
    DELETING,
    DELETED,
    DELETE_REJECTED;
}
