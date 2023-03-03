package network.parthenon.amcdb.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Database connection wrapper that executes all queries and statements on a background thread.
 */
public class BackgroundConnection implements network.parthenon.amcdb.data.Connection {
    protected final java.sql.Connection dbConnection;

    private final ExecutorService dbExecutor;

    /**
     * Creates a new BackgroundConnection with the specified (thread) name.
     * @param dbConnection JDBC connection
     * @param name         Connection name (will be set on the background thread)
     */
    public BackgroundConnection(java.sql.Connection dbConnection, String name) {
        this.dbConnection = dbConnection;
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName(name);
            return thread;
        });
    }

    /**
     * Executes the provided query and returns the result(s), closing the ResultSet.
     * @param resultTransform Function transforming a ResultSet to the return object type.
     * @param sql             SQL statement to execute.
     * @param params          Parameter values for the statement.
     * @return                Transformed result set.
     * @param <T>             Return object type.
     * @throws SQLException
     */
    public <T> CompletableFuture<T> query(Function<ResultSet, T> resultTransform, String sql, Object... params) {
        CompletableFuture<T> retrieval = new CompletableFuture<>();

        dbExecutor.submit(() -> {
            try(PreparedStatement stmt = dbConnection
                    .prepareStatement(sql)) {
                for(int i = 0; i < params.length; i++) {
                    // placeholders are 1-indexed
                    stmt.setObject(i+1, params[i]);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    retrieval.complete(resultTransform.apply(rs));
                }
            }
            catch(Exception e) {
                retrieval.completeExceptionally(e);
            }
        });

        return retrieval;
    }

    public CompletableFuture<Integer> execute(String sql, Object... params) {
        CompletableFuture<Integer> completion = new CompletableFuture<>();

        dbExecutor.submit(() -> {
            try(PreparedStatement stmt = dbConnection
                    .prepareStatement(sql)) {
                for(int i = 0; i < params.length; i++) {
                    // placeholders are 1-indexed
                    stmt.setObject(i+1, params[i]);
                }
                completion.complete(stmt.executeUpdate());
            }
            catch(Exception e) {
                completion.completeExceptionally(e);
            }
        });

        return completion;
    }

    @Override
    public void close() {
        try {
            this.dbConnection.close();
        }
        catch(SQLException e) {
            // nothing we can really do at this point
        }
    }
}
