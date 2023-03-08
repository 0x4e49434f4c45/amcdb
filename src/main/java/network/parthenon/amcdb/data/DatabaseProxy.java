package network.parthenon.amcdb.data;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.TransactionalCallable;
import org.jooq.TransactionalRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface DatabaseProxy {

    /**
     * Runs a database transaction asynchronously.
     * @param action           Function which accepts a jOOQ Configuration, performs some persistence action,
     *                         and returns a value.
     * @return                 CompletableFuture which will contain the value returned by the action.
     * @param <TReturn>        The type returned by the action.
     */
    <TReturn> CompletableFuture<TReturn> asyncTransaction(
            TransactionalCallable<TReturn> action);

    /**
     * Runs some SQL asynchronously outside a database transaction.
     * Used for DDL statements.
     * @param action Function which accepts a jOOQ Configuration, performs some persistence action,
     *               and returns a value.
     * @return       CompletableFuture which will contain the value returned by the action.
     * @param <TReturn> The type returned by the action.
     */
    <TReturn> CompletableFuture<TReturn> asyncBare(
            Function<Configuration, TReturn> action);
}
