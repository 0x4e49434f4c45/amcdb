package network.parthenon.amcdb.data;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DatabaseProxy {

    /**
     * Runs a database transaction asynchronously.
     * @param action           Function which accepts a ConnectionSource, performs some persistence action,
     *                         and returns a value.
     * @return                 CompletableFuture which will contain the value returned by the action.
     * @param <TPersistence>   The type on which the DAO operations.
     * @param <TReturn>        The type returned by the action.
     */
    <TPersistence,TReturn> CompletableFuture<TReturn> asyncTransaction(
            Function<ConnectionSource, TReturn> action);

    /**
     * Runs a database transaction asynchronously.
     * @param persistenceClass The class on which the DAO operates.
     * @param action           Function which accepts a DAO, performs some persistence action,
     *                         and returns a value.
     * @return                 CompletableFuture which will contain the value returned by the action.
     * @param <TPersistence>   The type on which the DAO operations.
     * @param <TReturn>        The type returned by the action.
     */
    <TPersistence,TReturn> CompletableFuture<TReturn> asyncTransaction(
            Class<TPersistence> persistenceClass,
            Function<Dao<TPersistence, ?>, TReturn> action);

    /**
     * Closes the underlying database connection(s).
     */
    void close();
}
