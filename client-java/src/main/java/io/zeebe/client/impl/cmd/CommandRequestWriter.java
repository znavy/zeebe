package io.zeebe.client.impl.cmd;

import static io.zeebe.protocol.clientapi.ExecuteCommandRequestEncoder.commandHeaderLength;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.client.TopicMetadata;
import io.zeebe.client.event.Event;
import io.zeebe.protocol.clientapi.ExecuteCommandRequestEncoder;
import io.zeebe.protocol.clientapi.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.io.ExpandableDirectBufferOutputStream;

public class CommandRequestWriter
{
    protected final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    protected final ExecuteCommandRequestEncoder commandRequestEncoder = new ExecuteCommandRequestEncoder();
    private final ObjectMapper objectMapper;

    public CommandRequestWriter(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
    }

    public int write(Event command, ExpandableArrayBuffer buffer)
    {
        final TopicMetadata metadata = command.getTopicMetadata();

        int writeOffset = 0;

        headerEncoder.wrap(buffer, writeOffset)
            .blockLength(commandRequestEncoder.sbeBlockLength())
            .schemaId(commandRequestEncoder.sbeSchemaId())
            .templateId(commandRequestEncoder.sbeTemplateId())
            .version(commandRequestEncoder.sbeSchemaVersion());

        writeOffset += headerEncoder.encodedLength();

        commandRequestEncoder.wrap(buffer, writeOffset)
            .partitionId(metadata.getPartitionId())
            .key(metadata.getKey())
            .eventType(metadata.getEventType())
            .topicName(metadata.getTopicName());

        final int serializedCommandOffset = commandRequestEncoder.limit() + commandHeaderLength();

        final ExpandableDirectBufferOutputStream out = new ExpandableDirectBufferOutputStream(buffer, serializedCommandOffset);

        try
        {
            objectMapper.writeValue(out, command);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unexpected exception while serializing command", e);
        }

        final int commandLength = out.position();

        buffer.putShort(writeOffset, (short) commandLength, java.nio.ByteOrder.LITTLE_ENDIAN);

        return serializedCommandOffset + commandLength;
    }
}
