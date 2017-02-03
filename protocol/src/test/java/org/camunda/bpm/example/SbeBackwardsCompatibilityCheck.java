package org.camunda.bpm.example;

import org.camunda.tngp.protocol.clientapi2.ExecuteCommandRequestDecoder;
import org.camunda.tngp.protocol.clientapi2.MessageHeaderDecoder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.protocol.clientapi.ExecuteCommandRequestEncoder;
import org.camunda.tngp.protocol.clientapi.MessageHeaderEncoder;

public class SbeBackwardsCompatibilityCheck
{
    MessageHeaderEncoder v1HeaderEncoder = new MessageHeaderEncoder();
    ExecuteCommandRequestEncoder v1BodyEncoder = new ExecuteCommandRequestEncoder();
    MessageHeaderDecoder v2HeaderDecoder = new MessageHeaderDecoder();
    ExecuteCommandRequestDecoder v2BodyDecoder = new ExecuteCommandRequestDecoder();


    @Test
    public void testEncodeDecode()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);
        Arrays.fill(buffer.byteArray(), (byte) 100);

        v1HeaderEncoder.wrap(buffer, 0)
            .blockLength(v1BodyEncoder.sbeBlockLength())
            .schemaId(v1BodyEncoder.sbeSchemaId())
            .templateId(v1BodyEncoder.sbeTemplateId())
            .version(v1BodyEncoder.sbeSchemaVersion());

        v1BodyEncoder.wrap(buffer, v1HeaderEncoder.encodedLength())
            .topicId(1L)
            .longKey(2L)
            .bytesKey("foo")
            .command("bar");

        v2HeaderDecoder.wrap(buffer, 0);
        Assert.assertEquals(v1BodyEncoder.sbeBlockLength(), v2HeaderDecoder.blockLength());
        Assert.assertEquals(v1BodyEncoder.sbeSchemaId(), v2HeaderDecoder.schemaId());
        Assert.assertEquals(v1BodyEncoder.sbeTemplateId(), v2HeaderDecoder.templateId());
        Assert.assertEquals(v1BodyEncoder.sbeSchemaVersion(), v2HeaderDecoder.version());

        v2BodyDecoder.wrap(buffer, v2HeaderDecoder.encodedLength(), v2HeaderDecoder.blockLength(), v2HeaderDecoder.version());

        Assert.assertEquals(1L, v2BodyDecoder.topicId());
        Assert.assertEquals(2L, v2BodyDecoder.longKey());
        Assert.assertEquals("foo", v2BodyDecoder.bytesKey());
        Assert.assertEquals("bar", v2BodyDecoder.command());
        Assert.assertEquals("", v2BodyDecoder.newDataField());
    }
}
