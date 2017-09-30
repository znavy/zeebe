/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.metrics.prometheus;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.MappedByteBuffer;

import com.sun.net.httpserver.*;
import org.agrona.BitUtil;
import org.agrona.IoUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.status.CountersManager;

@SuppressWarnings("restriction")
public class PrometheusNodeExporter
{
    public static final int COUNTERS_FILE_SIZE = 1024 * 1024 * 4;
    public static final int LABELS_BUFFER_OFFSET = 0;
    public static final int LABELS_BUFFER_SIZE = (int) (COUNTERS_FILE_SIZE * 0.75);
    public static final int COUNTERS_BUFFER_OFFSET = BitUtil.align(LABELS_BUFFER_SIZE, 8);
    public static final int COUNTERS_BUFFER_SIZE = COUNTERS_FILE_SIZE - COUNTERS_BUFFER_OFFSET;
    public static final String COUNTERS_FILE_NAME = "metrics.zeebe";

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1)
        {
            throw new RuntimeException("must provide location of metrics file as argument");
        }

        final String metricsFileName = args[0];

        if (metricsFileName.isEmpty())
        {
            throw new RuntimeException("must provide location of metrics file as argument");
        }

        final File countersFile = new File(metricsFileName);
        if (!countersFile.exists())
        {
            throw new RuntimeException(String.format("Metrics file %s does not exist", countersFile.getAbsolutePath()));
        }

        final InetSocketAddress inetSocketAddress = new InetSocketAddress(8000);
        final HttpServer server = HttpServer.create(inetSocketAddress, 0);

        System.out.println(String.format("Serving metrics at %s.", inetSocketAddress));

        countersFile.mkdirs();

        final MappedByteBuffer mappedCountersFile = IoUtil.mapExistingFile(countersFile, "metrics file");

        final UnsafeBuffer labelsBuffer = new UnsafeBuffer(mappedCountersFile, LABELS_BUFFER_OFFSET, LABELS_BUFFER_SIZE);
        final UnsafeBuffer countersBuffer = new UnsafeBuffer(mappedCountersFile, COUNTERS_BUFFER_OFFSET, COUNTERS_BUFFER_SIZE);

        final CountersManager countersManager = new CountersManager(labelsBuffer, countersBuffer);

        server.createContext("/metrics", new MyHandler(countersManager));
        server.setExecutor(null);
        server.start();

    }

    static class MyHandler implements HttpHandler
    {
        private final CountersManager countersManager;

        MyHandler(CountersManager countersManager)
        {
            this.countersManager = countersManager;
        }

        @Override
        public void handle(HttpExchange t) throws IOException
        {
            final StringBuilder stringBuilder = new StringBuilder();

            countersManager.forEach((id, label) ->
            {
                final String[] parts = label.split("\\.");
                final String metricName = parts[parts.length - 1];

                stringBuilder.append(metricName);
                stringBuilder.append("");

                if ("position".equals(metricName) || "limit".equals(metricName))
                {
                    stringBuilder.append("{");
                    stringBuilder.append("bufferName=\"");
                    for (int i = 0; i < parts.length - 2; i++)
                    {
                        stringBuilder.append(parts[i]);
                        if (i < parts.length - 3)
                        {
                            stringBuilder.append("_");
                        }
                    }
                    stringBuilder.append("\"}");
                }
                else if ("processedEvents".equals(metricName) || "skippedEvents".equals(metricName))
                {
                    stringBuilder.append("{");
                    stringBuilder.append("processor=\"");
                    stringBuilder.append(parts[3]);
                    stringBuilder.append("\",topic=\"");
                    stringBuilder.append(parts[0]);
                    stringBuilder.append(":");
                    stringBuilder.append(parts[1]);
                    stringBuilder.append("\"}");
                }
                else if ("transportBytesReceived".equals(metricName) || "transportBytesSent".equals(metricName))
                {
                    stringBuilder.append("{");
                    stringBuilder.append("transport=\"");
                    stringBuilder.append(parts[0]);
                    stringBuilder.append("_");
                    stringBuilder.append(parts[1]);
                    stringBuilder.append("\"}");
                }
                else if ("actorInvocationCount".equals(metricName))
                {
                    stringBuilder.append("{");
                    stringBuilder.append("actorName=\"");
                    for (int i = 0; i < parts.length - 1; i++)
                    {
                        stringBuilder.append(parts[i]);
                        if (i < parts.length - 2)
                        {
                            stringBuilder.append("_");
                        }
                    }
                    stringBuilder.append("\"}");
                }

                stringBuilder.append(" ");
                stringBuilder.append(countersManager.getCounterValue(id));
                stringBuilder.append("\n");
            });

            final String response = stringBuilder.toString();
            t.sendResponseHeaders(200, response.length());

            final OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
