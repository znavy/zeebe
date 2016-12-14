package org.camunda.tngp.client.impl.data;

import java.io.InputStream;
import java.io.OutputStream;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class JacksonDocumentConverter implements DocumentConverter
{
    protected JsonFactory msgPackFactory;
    protected JsonFactory jsonFactory;

    public JacksonDocumentConverter(JsonFactory msgPackFactory, JsonFactory jsonFactory)
    {
        this.msgPackFactory = msgPackFactory;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public void convertToMsgPack(InputStream jsonStream, OutputStream outStream) throws Exception
    {
        convert(jsonStream, outStream, jsonFactory, msgPackFactory);
    }

    @Override
    public void convertToJson(InputStream msgPackStream, OutputStream outStream) throws Exception
    {
        convert(msgPackStream, outStream, msgPackFactory, jsonFactory);
    }

    protected void convert(InputStream in, OutputStream out, JsonFactory inFormat, JsonFactory outFormat) throws Exception
    {
        final JsonParser parser = inFormat.createParser(in);
        final JsonGenerator generator = outFormat.createGenerator(out);
        final JsonToken token = parser.nextToken();
        if (token != JsonToken.START_OBJECT && token != JsonToken.START_ARRAY)
        {
            throw new RuntimeException("Document does not begin with an object or array");
        }

        generator.copyCurrentStructure(parser);

        if (parser.nextToken() != null)
        {
            throw new RuntimeException("Document has more content than a single object/array");
        }

        generator.flush();
    }

    public static JacksonDocumentConverter newDefaultConverter()
    {
        return new JacksonDocumentConverter(new MessagePackFactory(), new MappingJsonFactory());
    }

}
