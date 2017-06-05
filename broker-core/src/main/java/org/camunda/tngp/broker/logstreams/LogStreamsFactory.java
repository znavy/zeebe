package org.camunda.tngp.broker.logstreams;

import static org.camunda.tngp.util.EnsureUtil.*;

import java.io.File;
import java.util.Random;

import org.agrona.DirectBuffer;
import org.camunda.tngp.broker.logstreams.cfg.LogStreamsCfg;
import org.camunda.tngp.broker.system.threads.AgentRunnerServices;
import org.camunda.tngp.logstreams.LogStreams;
import org.camunda.tngp.logstreams.fs.FsLogStreamBuilder;
import org.camunda.tngp.logstreams.log.LogStream;

/**
 * A factory for logstreams. The factory is configured via {@link LogStreamsCfg}.
 * It has two responsibilities
 *
 * <ul>
 * <li>Creating new logstreams, allocating a directory based on the configuration</li>
 * <li>Opening existing logstreams</li>
 * </ul>
 *
 */
public class LogStreamsFactory
{
    private static final Random DIRECTORY_SEQUENCE = new Random();

    protected LogStreamsCfg logStreamsCfg;
    protected AgentRunnerServices agentRunner;

    public LogStreamsFactory(final LogStreamsCfg logStreamsCfg, final AgentRunnerServices agentRunner)
    {
        this.logStreamsCfg = logStreamsCfg;
        this.agentRunner = agentRunner;
    }

    /**
     * Create a new log stream. Automatically assigns a directory to the log stream based on this factory's configuration.
     *
     * @param topicName the name of the topic
     * @param partitionId the partition id
     * @return the opened log stream.
     */
    public LogStream createLogStream(final DirectBuffer topicName, final int partitionId)
    {
        ensureNotNullOrEmpty("topic name", topicName);
        ensureGreaterThanOrEqual("partition id", partitionId, 0);
        ensureLessThanOrEqual("partition id", partitionId, Short.MAX_VALUE);

        final FsLogStreamBuilder logStreamBuilder = LogStreams.createFsLogStream(topicName, partitionId);
        final String logName = logStreamBuilder.getLogName();

        final String logDirectory = assignDirectory(logName);

        return openLogStream(topicName, partitionId, logDirectory);
    }

    /**
     * Assigns a directory based on this factory's configuration
     *
     * @param logName The name of the logstream
     * @return the assigned directory path
     */
    private String assignDirectory(final String logName)
    {
        int assignedLogDirectory = 0;

        if (logStreamsCfg.directories.length == 0)
        {
            throw new RuntimeException(String.format("Cannot create logstream %s, no log directory provided.", logName));
        }
        else if (logStreamsCfg.directories.length > 1)
        {
            assignedLogDirectory = DIRECTORY_SEQUENCE.nextInt(logStreamsCfg.directories.length - 1);
        }

        return logStreamsCfg.directories[assignedLogDirectory] + File.separator + logName;
    }

    /**
     * Open an existing log stream from a directory
     *
     * @param topicName the name of the topic
     * @param partitionId the partition id
     * @param logDirectory the directory
     * @return the {@link LogStream}
     */
    public LogStream openLogStream(final DirectBuffer topicName, final int partitionId, final String logDirectory)
    {
        ensureNotNullOrEmpty("topic name", topicName);
        ensureGreaterThanOrEqual("partition id", partitionId, 0);
        ensureLessThanOrEqual("partition id", partitionId, Short.MAX_VALUE);

        final LogStream logStream = LogStreams.createFsLogStream(topicName, partitionId)
            .deleteOnClose(false)
            .logDirectory(logDirectory)
            .agentRunnerService(agentRunner.logAppenderAgentRunnerService())
            .logSegmentSize(logStreamsCfg.defaultLogSegmentSize * 1024 * 1024)
            .logStreamControllerDisabled(true)
            .build();

        // TODO: this is blocking :(
        logStream.open();

        return logStream;
    }
}
