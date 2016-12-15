package org.camunda.tngp.client.cmd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import org.camunda.tngp.client.impl.ClientCmdExecutor;
import org.camunda.tngp.client.impl.cmd.ClientResponseHandler;
import org.camunda.tngp.client.impl.cmd.CompleteTaskCmdImpl;
import org.camunda.tngp.client.impl.cmd.TaskAckResponseHandler;
import org.camunda.tngp.client.impl.cmd.taskqueue.CompleteTaskRequestWriter;
import org.camunda.tngp.client.impl.data.DocumentConverter;
import org.camunda.tngp.client.impl.data.JacksonDocumentConverter;
import org.camunda.tngp.util.buffer.BufferWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CompleteTaskInstanceCmdTest
{
    @Mock
    protected CompleteTaskRequestWriter requestWriter;

    @Mock
    protected ClientCmdExecutor commandExecutor;

    protected static final byte[] JSON_PAYLOAD = "{}".getBytes(StandardCharsets.UTF_8);
    protected static final byte[] MSG_PACK_PAYLOAD = new byte[] { (byte) 0x80 }; // FIXMAP with 0 elements

    protected DocumentConverter documentConverter = JacksonDocumentConverter.newDefaultConverter();

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldSetProperties()
    {
        // given
        final CompleteTaskCmdImpl command = new CompleteTaskCmdImpl(commandExecutor, documentConverter);
        command.setRequestWriter(requestWriter);
        final CompleteAsyncTaskCmd apiCommand = command;

        // when
        apiCommand.taskId(1234L);
        apiCommand.taskQueueId(1234);

        // then
        verify(requestWriter).taskId(1234L);
        verify(requestWriter).resourceId(1234);

    }

    @Test
    public void shouldConvertPayload()
    {
        // given
        final CompleteTaskCmdImpl command = new CompleteTaskCmdImpl(commandExecutor, documentConverter);
        command.setRequestWriter(requestWriter);
        final CompleteAsyncTaskCmd apiCommand = command;

        // when
        apiCommand.payload(JSON_PAYLOAD);

        // then
        verify(requestWriter).payload(eq(MSG_PACK_PAYLOAD), eq(0), eq(MSG_PACK_PAYLOAD.length));
    }

    @Test
    public void testRequestWriter()
    {
        // given
        final CompleteTaskCmdImpl command = new CompleteTaskCmdImpl(commandExecutor, documentConverter);

        // when
        final BufferWriter requestWriter = command.getRequestWriter();

        // then
        assertThat(requestWriter).isInstanceOf(CompleteTaskRequestWriter.class);
    }

    @Test
    public void testResponseHandlers()
    {
        // given
        final CompleteTaskCmdImpl command = new CompleteTaskCmdImpl(commandExecutor, documentConverter);

        // when
        final ClientResponseHandler<Long> responseHandler = command.getResponseHandler();

        // then
        assertThat(responseHandler).isInstanceOf(TaskAckResponseHandler.class);
    }

}
