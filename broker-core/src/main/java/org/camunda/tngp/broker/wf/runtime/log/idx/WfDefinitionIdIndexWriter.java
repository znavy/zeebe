package org.camunda.tngp.broker.wf.runtime.log.idx;

import org.camunda.tngp.broker.log.LogEntryHeaderReader;
import org.camunda.tngp.broker.log.Templates;
import org.camunda.tngp.broker.log.idx.IndexWriter;
import org.camunda.tngp.broker.services.HashIndexManager;
import org.camunda.tngp.broker.wf.runtime.log.WfDefinitionRuntimeRequestReader;
import org.camunda.tngp.hashindex.Long2LongHashIndex;
import org.camunda.tngp.taskqueue.data.WfDefinitionRuntimeRequestDecoder;

public class WfDefinitionIdIndexWriter implements IndexWriter
{

    protected HashIndexManager<Long2LongHashIndex> indexManager;
    protected Long2LongHashIndex index;
    protected Templates templates;

    public WfDefinitionIdIndexWriter(HashIndexManager<Long2LongHashIndex> indexManager, Templates templates)
    {
        this.indexManager = indexManager;
        this.index = indexManager.getIndex();
        this.templates = templates;
    }


    @Override
    public void indexLogEntry(long position, LogEntryHeaderReader reader)
    {
        if (reader.templateId() == WfDefinitionRuntimeRequestDecoder.TEMPLATE_ID)
        {
            final WfDefinitionRuntimeRequestReader requestReader = templates.getReader(Templates.WF_DEFINITION_RUNTIME_REQUEST);
            reader.readInto(requestReader);

            index.put(requestReader.id(), position);
        }
    }

    @Override
    public HashIndexManager<?> getIndexManager()
    {
        return indexManager;
    }

}