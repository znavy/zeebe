package org.camunda.tngp.broker.wf.runtime.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.broker.test.util.BufferAssert.assertThatBuffer;

import java.nio.charset.StandardCharsets;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;

public class JacksonJsonDocumentTest
{
    protected ObjectMapper objectMapper;
    protected Configuration jsonPathConfiguration;

    @Before
    public void setUp()
    {
        objectMapper = new ObjectMapper();
        jsonPathConfiguration = TngpJsonPath.getConfiguration();
    }

    @Test
    public void shouldProcessJsonPath()
    {
        // given
        final JacksonJsonDocument jsonDocument = new JacksonJsonDocument(objectMapper, jsonPathConfiguration, 1);
        final DirectBuffer jsonBuffer = encode("{\"key\":\"value\"}");
        jsonDocument.wrap(jsonBuffer, 0, jsonBuffer.capacity());

        final DirectBuffer jsonPathBuffer = encode("$.key");

        // when
        final JsonPathResult jsonPathResult = jsonDocument.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());

        // then
        assertThat(jsonPathResult.hasResolved()).isTrue();
        assertThat(jsonPathResult.isString()).isTrue();
        assertThatBuffer(jsonPathResult.asEncodedString()).hasBytes("value".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void shouldNotifyOfUnresolvedJsonPath()
    {
        // given
        final JacksonJsonDocument jsonDocument = new JacksonJsonDocument(objectMapper, jsonPathConfiguration, 1);
        final DirectBuffer jsonBuffer = encode("{}");
        jsonDocument.wrap(jsonBuffer, 0, jsonBuffer.capacity());

        final DirectBuffer jsonPathBuffer = encode("blablaa");

        // when
        final JsonPathResult jsonPathResult = jsonDocument.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());

        // then
        assertThat(jsonPathResult.hasResolved()).isFalse();
    }

    @Test
    public void shouldReuseResultInstancesRoundRobin()
    {
        // given
        final JacksonJsonDocument jsonDocument = new JacksonJsonDocument(objectMapper, jsonPathConfiguration, 2);
        final DirectBuffer jsonBuffer = encode("{\"key\":\"value\"}");
        jsonDocument.wrap(jsonBuffer, 0, jsonBuffer.capacity());

        final DirectBuffer jsonPathBuffer = encode("$.key");

        // when
        final JsonPathResult result1 = jsonDocument.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());
        final JsonPathResult result2 = jsonDocument.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());
        final JsonPathResult result3 = jsonDocument.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());
        final JsonPathResult result4 = jsonDocument.jsonPath(jsonPathBuffer, 0, jsonPathBuffer.capacity());

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
