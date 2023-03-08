package network.parthenon.amcdb.data.schema;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import network.parthenon.amcdb.data.DatabaseProxy;
import network.parthenon.amcdb.data.DatabaseProxyImpl;
import network.parthenon.amcdb.data.entities.AMCDBMetadata;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class MigrationTest {

    /**
     * Current schema version. Update whenever a new migration is added.
     */
    private static String TARGET_SCHEMA_VERSION = "1";

    private HikariDataSource hikariDataSource;
    private DatabaseProxy db;

    @BeforeEach
    public void setUpDatabase() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:amcdb-test");
        hikariDataSource = new HikariDataSource(config);
        db = new DatabaseProxyImpl(hikariDataSource, SQLDialect.H2, "AMCDB Persistence");
    }

    @AfterEach
    public void destroyDatabase() {
        hikariDataSource.close();
    }

    /**
     * Tests that the schema version is correct after applying migrations.
     */
    @Test
    public void testSchemaVersion() {
        db.asyncBare(Migration::applyMigrations).join();

        verifySchemaVersionAndLockReleased(getMetadata());
    }

    /**
     * Tests that when a migration has already been applied,
     * additional migrations are applied without issue and the schema version is correct.
     */
    @Test
    public void testUpgrade() {
        performInitialMigration();
        db.asyncBare(Migration::applyMigrations).join();

        verifySchemaVersionAndLockReleased(getMetadata());
    }

    /**
     * Performs three simultaneous migration attempts and validates
     * that in the end no errors occurred, the schema version is correct,
     * and the migration lock is released.
     */
    @Test
    public void testSimultaneousMigrations() {

        // Apply the very first migration and set the lock value to false.
        // Simultaneous migration from scratch cannot be supported.
        performInitialMigration();

        CompletableFuture.allOf(
                db.asyncBare(Migration::applyMigrations),
                db.asyncBare(Migration::applyMigrations),
                db.asyncBare(Migration::applyMigrations))
            .join();

        verifySchemaVersionAndLockReleased(getMetadata());
    }

    /**
     * Tests that an exception is thrown with an appropriate message
     * when the version indicated in the database is higher than the available
     * migrations.
     */
    @Test
    public void testFutureSchemaVersion() {
        performInitialMigration();
        db.asyncTransactionResult(conf -> {
            setMetadata(AMCDBMetadata.SCHEMA_VERSION, "1000000", conf);
            return null;
        }).join();
        assertThrows(RuntimeException.class,
                () -> db.asyncBare(Migration::applyMigrations).join(),
                "This database has been used with a later version of AMCDB and is not compatible with this version.\n" +
                "Database version: %d, this version of AMCDB implements database version %s.".formatted(1000000, TARGET_SCHEMA_VERSION));
    }

    /**
     * Performs the minimum viable migration.
     */
    private void performInitialMigration() {
        db.asyncBare(conf -> {
            new Migration.V0_Metadata().apply(conf);
            conf.dsl().batch(conf.dsl().insertInto(AMCDBMetadata.TABLE, AMCDBMetadata.KEY, AMCDBMetadata.VALUE)
                            .values((String) null, (String) null))
                    .bind(AMCDBMetadata.SCHEMA_VERSION, "0")
                    .bind(AMCDBMetadata.MIGRATION_LOCK, "false")
                    .execute();
        }).join();
    }

    private void verifySchemaVersionAndLockReleased(Map<String, String> metadata) {
        assertEquals(TARGET_SCHEMA_VERSION, metadata.get(AMCDBMetadata.SCHEMA_VERSION));
        assertEquals("false", metadata.get(AMCDBMetadata.MIGRATION_LOCK));
    }

    private Map<String, String> getMetadata() {
        return db.asyncTransactionResult(conf -> {
                    return conf.dsl().select(AMCDBMetadata.KEY, AMCDBMetadata.VALUE)
                            .from(AMCDBMetadata.TABLE)
                            .fetch()
                            .intoMap(AMCDBMetadata.KEY, AMCDBMetadata.VALUE);
                })
                .join();
    }

    private void setMetadata(String key, String value, Configuration conf) {
        int numUpdates = conf.dsl().update(AMCDBMetadata.TABLE)
                .set(AMCDBMetadata.VALUE, value)
                .where(AMCDBMetadata.KEY.eq(key))
                .execute();

        if(numUpdates == 0) {
            conf.dsl().insertInto(AMCDBMetadata.TABLE)
                    .set(AMCDBMetadata.KEY, key)
                    .set(AMCDBMetadata.VALUE, value)
                    .execute();
        }
    }
}