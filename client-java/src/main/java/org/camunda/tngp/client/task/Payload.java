package org.camunda.tngp.client.task;

import java.io.InputStream;

public interface Payload
{

    /**
     * @return an input stream containg the payload. JSON payload is UTF-8 encoded.
     */
    InputStream getRaw();

    /**
     * @return the number of bytes the stream returned by {@link #getRaw()} provides.
     */
    int rawSize();
}
