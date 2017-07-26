package io.zeebe.client.impl;

import io.zeebe.client.impl.data.MsgPackConverter;

public class CommandContext
{
    protected MsgPackConverter msgPackConverter;

    public MsgPackConverter getMsgPackConverter()
    {
        return msgPackConverter;
    }

    public void setMsgPackConverter(MsgPackConverter msgPackConverter)
    {
        this.msgPackConverter = msgPackConverter;
    }
}
