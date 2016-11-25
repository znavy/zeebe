/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.tngp.client.event.impl.dto;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.client.event.Event;

public class UnknownEvent implements Event
{
    protected long position;

    protected final MutableDirectBuffer rawBuffer = new UnsafeBuffer(0, 0);

    protected int templateId;

    @Override
    public long getPosition()
    {
        return position;
    }

    @Override
    public DirectBuffer getRawBuffer()
    {
        return rawBuffer;
    }

    public int getTemplateId()
    {
        return templateId;
    }

    public void setTemplateId(int templateId)
    {
        this.templateId = templateId;
    }

    public void setPosition(long position)
    {
        this.position = position;
    }

    public void setRawBuffer(DirectBuffer rawBuffer)
    {
        this.rawBuffer.wrap(new byte[rawBuffer.capacity()]);
        this.rawBuffer.putBytes(0, rawBuffer, 0, rawBuffer.capacity());
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("UnknownEvent [position=");
        builder.append(position);
        builder.append(", templateId=");
        builder.append(templateId);
        builder.append(", eventLength=");
        builder.append(rawBuffer.capacity());
        builder.append("]");
        return builder.toString();
    }

}
