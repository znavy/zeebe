package org.camunda.bpm.example;

import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.protocol.clientapi.ExecuteCommandRequestDecoder;
import org.camunda.tngp.protocol.clientapi.MessageHeaderDecoder;
import org.camunda.tngp.protocol.clientapi2.ExecuteCommandRequestEncoder;
import org.camunda.tngp.protocol.clientapi2.MessageHeaderEncoder;
import org.junit.Assert;
import org.junit.Test;

public class SbeForwardCompatibilityCheck
{

    MessageHeaderDecoder v1HeaderDecoder = new MessageHeaderDecoder();
    ExecuteCommandRequestDecoder v1BodyDecoder = new ExecuteCommandRequestDecoder();
    MessageHeaderEncoder v2HeaderEncoder = new MessageHeaderEncoder();
    ExecuteCommandRequestEncoder v2BodyEncoder = new ExecuteCommandRequestEncoder();


    @Test
    public void testEncodeDecode()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);
        v2HeaderEncoder.wrap(buffer, 0)
            .blockLength(v2BodyEncoder.sbeBlockLength())
            .schemaId(v2BodyEncoder.sbeSchemaId())
            .templateId(v2BodyEncoder.sbeTemplateId())
            .version(v2BodyEncoder.sbeSchemaVersion());

        v2BodyEncoder.wrap(buffer, v2HeaderEncoder.encodedLength())
            .topicId(1L)
            .longKey(2L)
            .newField(3L);

//        final EventBehaviorMappingEncoder groupEncoder = v2BodyEncoder.eventBehaviorMappingCount(1);
//        groupEncoder
//            .next()
//            .behavioralAspect(1)
//            .event(2);

        v2BodyEncoder
            .bytesKey("foo")
            .command("bar");

        v1HeaderDecoder.wrap(buffer, 0);
        Assert.assertEquals(v2BodyEncoder.sbeBlockLength(), v1HeaderDecoder.blockLength());
        Assert.assertEquals(v2BodyEncoder.sbeSchemaId(), v1HeaderDecoder.schemaId());
        Assert.assertEquals(v2BodyEncoder.sbeTemplateId(), v1HeaderDecoder.templateId());
        Assert.assertEquals(v2BodyEncoder.sbeSchemaVersion(), v1HeaderDecoder.version());

        v1BodyDecoder.wrap(buffer, v1HeaderDecoder.encodedLength(), v1HeaderDecoder.blockLength(), v1HeaderDecoder.version());

        Assert.assertEquals(1L, v1BodyDecoder.topicId());
        Assert.assertEquals(2L, v1BodyDecoder.longKey());
        Assert.assertEquals("foo", v1BodyDecoder.bytesKey());
        Assert.assertEquals("bar", v1BodyDecoder.command());
    }

}
