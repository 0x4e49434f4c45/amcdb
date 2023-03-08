package network.parthenon.amcdb.data;

import org.jooq.Configuration;
import org.jooq.TransactionalCallable;
import org.jooq.TransactionalRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DatabaseProxy {

    /**
     * Runs a database transaction asynchronously.
     * @param action           Function which accepts a jOOQ Configuration, performs some persistence action,
     *                         and returns a value.
     * @return                 CompletableFuture which will contain the value returned by the action.
     * @param <TReturn>        The type returned by the action.
     */
    <TReturn> CompletableFuture<TReturn> asyncTransactionResult(
            TransactionalCallable<TReturn> action);

    /**
     * Runs a database transaction asynchronously.
     * @param action           Function which accepts a jOOQ Configuration, performs some persistence action,
     *                         and does not return a value.
     * @return                 CompletableFuture which will contain the value returned by the action.
     */
    default CompletableFuture<Void> asyncTransaction(TransactionalRunnable action) {
        return asyncTransactionResult(conf -> {
            action.run(conf);
            return null;
        });
    }

    /**
     * Runs some SQL asynchronously outside a database transaction.
     * Used for DDL statements.
     * @param action Function which accepts a jOOQ Configuration, performs some persistence action,
     *               and returns a value.
     * @return       CompletableFuture which will contain the value returned by the action.
     * @param <TReturn> The type returned by the action.
     */
    <TReturn> CompletableFuture<TReturn> asyncBareResult(
            Function<Configuration, TReturn> action);

    /**
     * Runs some SQL asynchronously outside a database transaction.
     * Used for DDL statements.
     * @param action Function which accepts a jOOQ Configuration, performs some persistence action,
     *               and does not return a value.
     * @return       CompletableFuture which will contain the value returned by the action.
     */
    default CompletableFuture<Void> asyncBare(Consumer<Configuration> action) {
        return asyncBareResult(conf -> {
            action.accept(conf);
            return null;
        });
    }
}
