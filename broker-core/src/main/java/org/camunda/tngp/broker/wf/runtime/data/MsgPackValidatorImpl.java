package org.camunda.tngp.broker.wf.runtime.data;

import org.agrona.DirectBuffer;
import org.camunda.tngp.msgpack.query.MsgPackTokenVisitor;
import org.camunda.tngp.msgpack.query.MsgPackTraverser;

public class MsgPackValidatorImpl implements MsgPackValidator
{
    protected static final MsgPackTokenVisitor DO_NOTHING_VISITOR = (p, c) ->
    { };

    protected MsgPackTraverser msgPackTraverser = new MsgPackTraverser();
    protected final ValidationResultImpl result = new ValidationResultImpl();

    @Override
    public MsgPackValidationResult validate(DirectBuffer buffer, int offset, int length)
    {
        msgPackTraverser.wrap(buffer, offset, length);
        final boolean success = msgPackTraverser.traverse(DO_NOTHING_VISITOR);

        result.isValid = success;
        if (success)
        {
            result.errorMessage = null;
        }
        else
        {
            result.errorMessage = "Error at position " + msgPackTraverser.getInvalidPosition() +
                    ": " + msgPackTraverser.getErrorMessage();
        }

        return result;
    }

    protected static class ValidationResultImpl implements MsgPackValidationResult
    {

        protected boolean isValid = true;
        protected String errorMessage = null;

        @Override
        public boolean isValid()
        {
            return isValid;
        }

        @Override
        public String getErrorMessage()
        {
            return errorMessage;
        }

    }

}
