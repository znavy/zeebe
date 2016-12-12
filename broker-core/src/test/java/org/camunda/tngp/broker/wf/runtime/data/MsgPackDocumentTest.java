package org.camunda.tngp.broker.wf.runtime.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.broker.test.util.BufferAssert.assertThatBuffer;

import java.nio.charset.StandardCharsets;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.broker.test.util.MsgPackUtil;
import org.junit.Test;

public class MsgPackDocumentTest
{

    @Test
    public void shouldProcessJsonPath()
    {
        // given
        final MsgPackDocument document = new MsgPackDocumentImpl(1);
        final DirectBuffer msgPackBuffer = MsgPackUtil.encodeMsgPack((p) ->
        {
            p.packMapHeader(1);
            p.packString("key");
            p.packString("value");
        });

        document.wrap(msgPackBuffer, 0, msgPackBuffer.capacity());

        final DirectBuffer jsonPathBuffer = encode("$.key");

        // when
        final JsonPathResult jsonPathResult = document.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());

        // then
        assertThat(jsonPathResult.hasResolved()).isTrue();
        assertThat(jsonPathResult.isString()).isTrue();
        assertThatBuffer(jsonPathResult.asEncodedString()).hasBytes("value".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void shouldNotifyOfUnresolvedJsonPath()
    {
        // given
        final MsgPackDocument document = new MsgPackDocumentImpl(1);
        final DirectBuffer msgPackBuffer = MsgPackUtil.encodeMsgPack((p) ->
        {
            p.packMapHeader(0);
        });
        document.wrap(msgPackBuffer, 0, msgPackBuffer.capacity());

        final DirectBuffer jsonPathBuffer = encode("$.blablaa");

        // when
        final JsonPathResult jsonPathResult = document.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());

        // then
        assertThat(jsonPathResult.hasResolved()).isFalse();
    }

    @Test
    public void shouldReuseResultInstancesRoundRobin()
    {
        // given
        final MsgPackDocument document = new MsgPackDocumentImpl(2);
        final DirectBuffer msgPackBuffer = MsgPackUtil.encodeMsgPack((p) ->
        {
            p.packMapHeader(1);
            p.packString("key");
            p.packString("value");
        });
        document.wrap(msgPackBuffer, 0, msgPackBuffer.capacity());

        final DirectBuffer jsonPathBuffer = encode("$.key");

        // when
        final JsonPathResult result1 = document.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());
        final JsonPathResult result2 = document.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());
        final JsonPathResult result3 = document.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());
        final JsonPathResult result4 = document.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());

        // then
        assertThat(result1).isNotSameAs(result2);
        assertThat(result1).isSameAs(result3);
        assertThat(result2).isSameAs(result4);
    }

    protected DirectBuffer encode(String value)
    {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return new UnsafeBuffer(bytes);
    }

}
