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
package io.zeebe.test.broker.protocol.clientapi;

import static io.zeebe.protocol.clientapi.ExecuteCommandResponseDecoder.eventHeaderLength;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import io.zeebe.protocol.clientapi.*;
import io.zeebe.test.broker.protocol.MsgPackHelper;
import io.zeebe.util.buffer.BufferReader;
import org.agrona.DirectBuffer;
import org.agrona.LangUtil;
import org.agrona.io.DirectBufferInputStream;

public class ExecuteCommandResponse implements BufferReader
{
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final ExecuteCommandResponseDecoder responseDecoder = new ExecuteCommandResponseDecoder();
    protected final ErrorResponse errorResponse;

    protected final MsgPackHelper msgPackHelper;

    protected Map<String, Object> event;

    public ExecuteCommandResponse(MsgPackHelper msgPackHelper)
    {
        this.msgPackHelper = msgPackHelper;
        this.errorResponse = new ErrorResponse(msgPackHelper);
    }

    public Map<String, Object> getEvent()
    {
        return event;
    }

    public long position()
    {
        return responseDecoder.position();
    }

    public long key()
    {
        return responseDecoder.key();
    }

    public int partitionId()
    {
        return responseDecoder.partitionId();
    }

    @Override
    public void wrap(DirectBuffer responseBuffer, int offset, int length)
    {
        messageHeaderDecoder.wrap(responseBuffer, offset);

        if (messageHeaderDecoder.templateId() != responseDecoder.sbeTemplateId())
        {
            if (messageHeaderDecoder.templateId() == ErrorResponseDecoder.TEMPLATE_ID)
            {
                errorResponse.wrap(responseBuffer, offset + messageHeaderDecoder.encodedLength(), length);
                throw new RuntimeException("Unexpected error response from broker: " +
                        errorResponse.getErrorCode() + " - " + errorResponse.getErrorData());
            }
            else
            {
                throw new RuntimeException("Unexpected response from broker. Template id " + messageHeaderDecoder.templateId());
            }
        }

        responseDecoder.wrap(responseBuffer, offset + messageHeaderDecoder.encodedLength(), messageHeaderDecoder.blockLength(), messageHeaderDecoder.version());

        final int eventLength = responseDecoder.eventLength();
        final int eventOffset = responseDecoder.limit() + eventHeaderLength();

        try (InputStream is = new DirectBufferInputStream(responseBuffer, eventOffset, eventLength))
        {
            event = msgPackHelper.readMsgPack(is);
        }
        catch (IOException e)
        {
            LangUtil.rethrowUnchecked(e);
        }
    }

}
