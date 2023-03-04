package network.parthenon.amcdb.util;

import network.parthenon.amcdb.AMCDB;

import java.util.function.BiFunction;

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
}
