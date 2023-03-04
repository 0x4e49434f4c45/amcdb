package network.parthenon.amcdb.data;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Database connection wrapper that executes all queries and statements on a background thread.
 */
public class DatabaseProxyImpl implements DatabaseProxy {
    protected final ConnectionSource dbConnectionSource;

    /**
     * Creates a new BackgroundDatabaseProxy with the specified (thread) name.
     * @param dbConnectionSource ORMLite connection source
     */
    public DatabaseProxyImpl(ConnectionSource dbConnectionSource) {
        this.dbConnectionSource = dbConnectionSource;
    }

    @Override
    public <TPersistence, TReturn> CompletableFuture<TReturn> asyncTransaction(
            Function<ConnectionSource, TReturn> action) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return TransactionManager.callInTransaction(dbConnectionSource, () -> action.apply(dbConnectionSource));
            }
            catch(SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public <TPersistence, TReturn> CompletableFuture<TReturn> asyncTransaction(
            Class<TPersistence> persistenceClass,
            Function<Dao<TPersistence, ?>, TReturn> action) {
        return asyncTransaction(action.compose(cs -> {
            try {
                return DaoManager.createDao(cs, persistenceClass);
            }
            catch(SQLException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @Override
    public void close() {
        try {
            dbConnectionSource.close();
        }
        catch(Exception e) {
            // we tried to close the connection source, if it can't be cleanly closed
            // there's nothing left to do since we're probably shutting down the server
            // anyway
        }
    }
}
