package org.camunda.bpm.broker.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.agrona.LangUtil;

public class TestUtil
{

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2L);
    public static final int DEFAULT_RETRIES = 20;

    public static <T> Invocation<T> doRepeatedly(Callable<T> callable)
    {
        return new Invocation<>(callable);
    }

    public static void waitUntil(final BooleanSupplier condition)
    {
        doRepeatedly(() -> null).until((r) -> condition.getAsBoolean());
    }


    public static void waitUntil(final BooleanSupplier condition, Duration timeout)
    {
        doRepeatedly(() -> null).until((r) -> condition.getAsBoolean(), timeout);
    }

    public static class Invocation<T>
    {
        protected Callable<T> callable;

        public Invocation(Callable<T> callable)
        {
            this.callable = callable;
        }


        public T until(Function<T, Boolean> resultCondition)
        {
            return until(resultCondition, (e) -> false);
        }

        public T until(Function<T, Boolean> resultCondition, Duration timeout)
        {
            return until(resultCondition, (e) -> false, timeout);
        }

        public T until(final Function<T, Boolean> resultCondition, Function<Exception, Boolean> exceptionCondition)
        {
            return until((t) -> resultCondition.apply(t), (e) -> exceptionCondition.apply(e), DEFAULT_TIMEOUT);
        }

        public T until(final Function<T, Boolean> resultCondition, Function<Exception, Boolean> exceptionCondition, Duration timeout)
        {
            final T result = whileConditionHolds((t) -> !resultCondition.apply(t), (e) -> !exceptionCondition.apply(e), DEFAULT_RETRIES, timeout);

            assertThat(resultCondition.apply(result)).isTrue();

            return result;
        }

        public T whileConditionHolds(Function<T, Boolean> resultCondition)
        {
            return whileConditionHolds(resultCondition, (e) -> true, DEFAULT_RETRIES, DEFAULT_TIMEOUT);
        }

        public T whileConditionHolds(
                Function<T, Boolean> resultCondition,
                Function<Exception, Boolean> exceptionCondition,
                int retries,
                Duration timeout)
        {
            int numTries = 0;
            final long timeoutPerRetry = timeout.toMillis() / retries;

            T result;

            do
            {
                result = null;

                try
                {
                    if (numTries > 0)
                    {
                        Thread.sleep(timeoutPerRetry);
                    }

                    result = callable.call();
                }
                catch (Exception e)
                {
                    if (!exceptionCondition.apply(e))
                    {
                        LangUtil.rethrowUnchecked(e);
                    }
                }

                numTries++;
            }
            while (numTries < retries && resultCondition.apply(result));

            return result;
        }
    }
}
