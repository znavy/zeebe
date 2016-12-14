package org.camunda.tngp.client.impl.data;

import static org.camunda.tngp.broker.test.util.BufferAssert.assertThatBuffer;

import java.nio.charset.StandardCharsets;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.io.DirectBufferInputStream;
import org.agrona.io.DirectBufferOutputStream;
import org.camunda.tngp.broker.test.util.MsgPackUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JacksonDocumentConverterTest
{

    @Rule
    public ExpectedException exception = ExpectedException.none();

    protected DocumentConverter converter = JacksonDocumentConverter.newDefaultConverter();

    protected static final DirectBuffer JSON = encode("{\"key1\":1,\"key2\":2}");
    protected static final DirectBuffer MSG_PACK = MsgPackUtil.encodeMsgPack((p) ->
    {
        p.packMapHeader(2);
        p.packString("key1");
        p.packInt(1);
        p.packString("key2");
        p.packInt(2);
    });

    @Test
    public void shouldConvertFromJsonToMsgPack() throws Exception
    {
        // given
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);
        final DirectBufferOutputStream outStream = new DirectBufferOutputStream(buffer);
        final DirectBufferInputStream inStream = new DirectBufferInputStream(JSON);

        // when
        converter.convertToMsgPack(inStream, outStream);

        // then
        assertThatBuffer(buffer).hasBytes(MSG_PACK, 0, MSG_PACK.capacity());
    }

    @Test
    public void shouldConvertFromMsgPackToJson() throws Exception
    {
        // given
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);
        final DirectBufferOutputStream outStream = new DirectBufferOutputStream(buffer);
        final DirectBufferInputStream inStream = new DirectBufferInputStream(MSG_PACK);

        // when
        converter.convertToJson(inStream, outStream);

        // then
        assertThatBuffer(buffer).hasBytes(JSON, 0, JSON.capacity());
    }

    @Test
    public void shouldThrowExceptionIfDocumentIsNotAJsonObject() throws Exception
    {
        // given
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);
        final DirectBufferOutputStream outStream = new DirectBufferOutputStream(buffer);
        final DirectBufferInputStream inStream = new DirectBufferInputStream(
                encode("123"));

        // then
        exception.expect(RuntimeException.class);
        exception.expectMessage("Document does not begin with an object or array");

        // when
        converter.convertToMsgPack(inStream, outStream);
    }

    @Test
    public void shouldThrowExceptionIfDocumentHasMoreThanOneObject() throws Exception
    {
        // given
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);
        final DirectBufferOutputStream outStream = new DirectBufferOutputStream(buffer);
        final DirectBufferInputStream inStream = new DirectBufferInputStream(
                encode("{}{}"));

        // then
        exception.expect(RuntimeException.class);
        exception.expectMessage("Document has more content than a single object/array");

        // when
        converter.convertToMsgPack(inStream, outStream);
    }

    protected static DirectBuffer encode(String string)
    {
        return new UnsafeBuffer(string.getBytes(StandardCharsets.UTF_8));
    }

}
