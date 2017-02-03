package org.camunda.bpm.example;

import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.protocol.clientapi.ErrorResponseDecoder;
import org.camunda.tngp.protocol.clientapi.MessageHeaderDecoder;
import org.camunda.tngp.protocol.clientapi2.ErrorCode;
import org.camunda.tngp.protocol.clientapi2.ErrorResponseEncoder;
import org.camunda.tngp.protocol.clientapi2.MessageHeaderEncoder;
import org.junit.Assert;
import org.junit.Test;

public class SbeForwardCompatibilityEnumCheck
{

    MessageHeaderDecoder v1HeaderDecoder = new MessageHeaderDecoder();
    ErrorResponseDecoder v1BodyDecoder = new ErrorResponseDecoder();
    MessageHeaderEncoder v2HeaderEncoder = new MessageHeaderEncoder();
    ErrorResponseEncoder v2BodyEncoder = new ErrorResponseEncoder();

    @Test
    public void testAdditionalAnnotationValue()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);
        v2HeaderEncoder.wrap(buffer, 0)
            .blockLength(v2BodyEncoder.sbeBlockLength())
            .schemaId(v2BodyEncoder.sbeSchemaId())
            .templateId(v2BodyEncoder.sbeTemplateId())
            .version(v2BodyEncoder.sbeSchemaVersion());

        v2BodyEncoder.wrap(buffer, v2HeaderEncoder.encodedLength())
            .errorCode(ErrorCode.NEW_ERROR_CODE)
            .errorData("foo")
            .failedRequest("bar");

        v1HeaderDecoder.wrap(buffer, 0);
        Assert.assertEquals(v2BodyEncoder.sbeBlockLength(), v1HeaderDecoder.blockLength());
        Assert.assertEquals(v2BodyEncoder.sbeSchemaId(), v1HeaderDecoder.schemaId());
        Assert.assertEquals(v2BodyEncoder.sbeTemplateId(), v1HeaderDecoder.templateId());
        Assert.assertEquals(v2BodyEncoder.sbeSchemaVersion(), v1HeaderDecoder.version());

        v1BodyDecoder.wrap(buffer, v1HeaderDecoder.encodedLength(), v1HeaderDecoder.blockLength(), v1HeaderDecoder.version());

        Assert.assertEquals(1L, v1BodyDecoder.errorCode());

    }

}
