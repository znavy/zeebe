package org.camunda.tngp.bpmn.graph;

import org.agrona.DirectBuffer;

// TODO: should go to util
public interface MsgPackScalarReader
{

    boolean isInteger();

    /**
     * MsgPack spec calls natural numbers integer; we represent them as values of type long here
     *
     * @return integer value as long
     */
    long asInteger();

    boolean isFloat();

    /**
     * MsgPack spec calls rational numbers float; we represent them as values of type double here
     *
     * @return float value as double
     */
    double asFloat();

    boolean isString();

    DirectBuffer asEncodedString();

    boolean isNil();

    boolean isBoolean();

    boolean asBoolean();
}
