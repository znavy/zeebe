package org.camunda.tngp.client.impl.data;

import java.io.InputStream;
import java.io.OutputStream;

public interface DocumentConverter
{

    void convertToMsgPack(InputStream jsonStream, OutputStream outStream) throws Exception;
    void convertToJson(InputStream msgPackStream, OutputStream outStream) throws Exception;

}
