package network.parthenon.amcdb.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface Connection {
    /**
     * Executes the provided query asynchronously and returns the result(s), closing the ResultSet.
     * @param resultTransform Function transforming a ResultSet to the return object type.
     * @param sql             SQL statement to execute.
     * @param params          Parameter values for the statement.
     * @return                CompletableFuture that will contain the transformed result set.
     * @param <T>             Return object type.
     * @throws SQLException
     */
    <T> CompletableFuture<T> query(Function<ResultSet, T> resultTransform, String sql, Object... params);

    /**
     * Executes the provided statement asynchronously and returns the number of rows modified.
     * @param sql    The statement to execute.
     * @param params Parameter values for the statement.
     * @return CompletableFuture that will contain the number of rows modified.
     */
    CompletableFuture<Integer> execute(String sql, Object... params);

    /**
     * Closes the backing database connection.
     */
    void close();
}
