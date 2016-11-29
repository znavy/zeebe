package org.camunda.tngp.broker.wf.runtime.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.tngp.broker.test.util.BufferAssert.assertThatBuffer;

import java.nio.charset.StandardCharsets;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.broker.services.JacksonJsonService;
import org.camunda.tngp.broker.services.JsonConfiguration;
import org.junit.Before;
import org.junit.Test;

public class JacksonJsonConfigurationTest
{
    protected JsonConfiguration jsonConfiguration;

    @Before
    public void setUp()
    {
        jsonConfiguration = JacksonJsonService.buildJsonConfiguration();
    }

    @Test
    public void shouldProcessJsonPath()
    {
        // given
        final JsonDocument jsonDocument = jsonConfiguration.buildJsonDocument(1);
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
        final JsonDocument jsonDocument = jsonConfiguration.buildJsonDocument(1);
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
        final JsonDocument jsonDocument = jsonConfiguration.buildJsonDocument(2);
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
