package network.parthenon.amcdb.data.services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import network.parthenon.amcdb.data.DatabaseProxy;
import network.parthenon.amcdb.data.DatabaseProxyImpl;
import network.parthenon.amcdb.data.schema.Migration;
import org.jooq.SQLDialect;

public abstract class DataServiceTestBase {

    protected static DatabaseProxy databaseProxy;

    protected static void setupDatabase(boolean doMigrations) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:amcdb-test");
        databaseProxy = new DatabaseProxyImpl(new HikariDataSource(config), SQLDialect.H2, "AMCDB Persistence");
        if(doMigrations) {
            databaseProxy.asyncBare(Migration::applyMigrations).join();
        }
    }
}
