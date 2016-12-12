package org.camunda.tngp.broker.wf.runtime.data;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.msgpack.jsonpath.JsonPathQuery;
import org.camunda.tngp.msgpack.jsonpath.JsonPathQueryCompiler;
import org.camunda.tngp.msgpack.query.MsgPackQueryExecutor;
import org.camunda.tngp.msgpack.query.MsgPackTraverser;

public class MsgPackDocumentImpl implements MsgPackDocument
{
    protected final UnsafeBuffer documentBuffer = new UnsafeBuffer(0, 0);
    protected JsonPathResultImpl[] jsonPathResults;
    protected int nextResult;

    protected JsonPathQueryCompiler jsonPathCompiler = new JsonPathQueryCompiler();
    protected MsgPackTraverser traverser = new MsgPackTraverser();
    protected MsgPackQueryExecutor tokenVisitor = new MsgPackQueryExecutor();

    /**
     * @param resultPoolSize the number of {@link JsonPathResultImpl} instances that can be held in parallel. The <code>resultPoolSize + 1</code>
     *   json path result is going to reuse the first result instance, etc. (round robin)
     */
    public MsgPackDocumentImpl(int resultPoolSize)
    {
        this.jsonPathResults = new JsonPathResultImpl[resultPoolSize];
        for (int i = 0; i < resultPoolSize; i++)
        {
            jsonPathResults[i] = new JsonPathResultImpl();
        }
        this.nextResult = 0;
    }

    /**
     * @return true if content is valid Jackson; false otherwise; in case false is returned, this method has no
     *   other effect
     */
    public void wrap(DirectBuffer buffer, int offset, int length)
    {
        documentBuffer.wrap(buffer, offset, length);
    }

    @Override
    public JsonPathResult jsonPath(DirectBuffer jsonPathBuffer, int offset, int length)
    {
        final JsonPathResultImpl nextResultObject = jsonPathResults[nextResult];
        nextResult = (nextResult + 1) % jsonPathResults.length;

        final JsonPathQuery jsonPathQuery = jsonPathCompiler.compile(jsonPathBuffer, offset, length);
        tokenVisitor.init(jsonPathQuery.getFilters(), jsonPathQuery.getFilterInstances());
        traverser.wrap(documentBuffer, 0, documentBuffer.capacity());
        traverser.traverse(tokenVisitor);

        nextResultObject.wrap(documentBuffer, tokenVisitor);
        return nextResultObject;
    }


}
