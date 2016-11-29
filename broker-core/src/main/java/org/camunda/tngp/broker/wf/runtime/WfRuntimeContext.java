package org.camunda.tngp.broker.wf.runtime;

import org.camunda.tngp.broker.log.LogConsumer;
import org.camunda.tngp.broker.log.LogWriter;
import org.camunda.tngp.broker.transport.worker.spi.ResourceContext;
import org.camunda.tngp.broker.wf.runtime.data.JsonValidator;
import org.camunda.tngp.log.Log;

public class WfRuntimeContext implements ResourceContext
{
    protected final int id;
    protected final String name;

    protected LogWriter logWriter;

    protected LogConsumer logConsumer;

    protected Log log;
    protected JsonValidator jsonValidator;

    public WfRuntimeContext(int id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public LogWriter getLogWriter()
    {
        return logWriter;
    }

    public void setLogWriter(LogWriter logWriter)
    {
        this.logWriter = logWriter;
    }

    public Log getLog()
    {
        return log;
    }

    public void setLog(Log log)
    {
        this.log = log;
    }

    @Override
    public int getResourceId()
    {
        return id;
    }

    @Override
    public String getResourceName()
    {
        return name;
    }

    public LogConsumer getLogConsumer()
    {
        return logConsumer;
    }
    public void setLogConsumer(LogConsumer logConsumer)
    {
        this.logConsumer = logConsumer;
    }

    public void setJsonValidator(JsonValidator jsonValidator)
    {
        this.jsonValidator = jsonValidator;
    }

    public JsonValidator getJsonValidator()
    {
        return jsonValidator;
    }
}
