package network.parthenon.amcdb.data;

import network.parthenon.amcdb.util.AsyncUtil;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.TransactionalCallable;
import org.jooq.impl.DefaultConfiguration;

import javax.sql.DataSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Database connection wrapper that executes all queries and statements on a background thread.
 */
public class DatabaseProxyImpl implements DatabaseProxy {
    protected final DataSource dbConnectionSource;

    protected final Configuration dbConfiguration;

    protected final ExecutorService persistencePool;

    /**
     * Creates a new BackgroundDatabaseProxy with the specified (thread) name.
     * @param dbConnectionSource DataSource from which to obtain JDBC connections
     * @param dialect            SQL dialect for jOOQ
     * @param poolName           Name for the persistence thread pool.
     */
    public DatabaseProxyImpl(DataSource dbConnectionSource, SQLDialect dialect, String poolName) {
        this.dbConnectionSource = dbConnectionSource;
        this.persistencePool = Executors.newCachedThreadPool(AsyncUtil.getNamedPoolThreadFactory(poolName));
        DefaultConfiguration dbConfiguration = new DefaultConfiguration();
        dbConfiguration.setDataSource(dbConnectionSource);
        dbConfiguration.setSQLDialect(dialect);
        dbConfiguration.setExecutor(persistencePool);
        this.dbConfiguration = dbConfiguration;
    }

    @Override
    public <TReturn> CompletableFuture<TReturn> asyncTransactionResult(
            TransactionalCallable<TReturn> action) {
        return this.dbConfiguration.dsl().transactionResultAsync(action).toCompletableFuture();
    }

    @Override
    public <TReturn> CompletableFuture<TReturn> asyncBareResult(Function<Configuration, TReturn> action) {
        return CompletableFuture.supplyAsync(() -> action.apply(dbConfiguration), persistencePool);
    }
}
