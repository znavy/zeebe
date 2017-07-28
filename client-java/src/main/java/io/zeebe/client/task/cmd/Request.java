package io.zeebe.client.task.cmd;

import java.util.concurrent.Future;

// TODO: package membership
public interface Request<E>
{

    /**
     * Executes the command and blocks until the result is available.
     * Throws {@link RuntimeException} in case the command times out.
     *
     * @return the result of the command.
     */
    E execute();

    /**
     * Executes the command asynchronously and returns control to the client thread.
     *
     * @return a future of the command result
     */
    Future<E> executeAsync();
}
