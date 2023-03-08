package network.parthenon.amcdb.util;

import network.parthenon.amcdb.AMCDB;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class AsyncUtil {

    /**
     * Logs the provided error and returns null of the specified type
     * (used with CompletableFuture(T).exceptionally()).
     * @param message Message to log with the error.
     * @param t       Throwable error to log.
     * @return        Null
     * @param <T>     Generic type parameter of the CompletableFuture(T)
     */
    public static <T> T logError(String message, Throwable t) {
        AMCDB.LOGGER.error(message, t);
        return null;
    }

    /**
     * Logs the provided error and returns null of the specified type
     * (used with CompletableFuture(T).exceptionally()).
     * @param t       Throwable error to log.
     * @return        Null
     * @param <T>     Generic type parameter of the CompletableFuture(T)
     */
    public static <T> T logError(Throwable t) {
        return logError(t.getMessage(), t);
    }

    /**
     * Gets a ThreadFactory that names threads with the given prefix and a sequential
     * numeric ID.
     * @param poolName Thread name prefix
     * @return ThreadFactory
     */
    public static ThreadFactory getNamedPoolThreadFactory(String poolName) {
        AtomicInteger threadNum = new AtomicInteger(1);
        ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        return r -> {
            Thread t = defaultFactory.newThread(r);
            t.setName("%s-%d".formatted(poolName, threadNum.getAndIncrement()));
            return t;
        };
    }
}
