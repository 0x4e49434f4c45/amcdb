package network.parthenon.amcdb.data.schema;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.data.schema.version.V1_CreateTables;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.SQLDataType;

import java.util.List;

import static org.jooq.impl.DSL.*;

public abstract class Migration {

    private static final Table<Record> AMCDB_METADATA = table(name("amcdb_metadata"));

    private static final Field<String> AMCDB_METADATA_KEY = field(name("key"), SQLDataType.VARCHAR);
    private static final Field<String> AMCDB_METADATA_VALUE = field(name("value"), SQLDataType.VARCHAR);

    private static final String SCHEMA_VERSION_KEY = "schema_version";
    private static final String MIGRATION_LOCK_KEY = "migration_lock";

    private static final List<Class<? extends Migration>> MIGRATIONS = List.of(
            V0_Metadata.class,
            V1_CreateTables.class
    );

    /**
     * Applies any new migrations to the database, and updates the stored version number.
     *
     * Do not invoke this method in a transaction! Many DBMS do not support DDL in a transaction.
     * @param conf jOOQ Configuration
     */
    public static void applyMigrations(Configuration conf) {
        int dbVersion = -1;
        boolean hasLock = false;
        try {
            if (!(conf.dsl().meta().getTables(AMCDB_METADATA.getName()).isEmpty())) {
                for (int attempts = 0; attempts < 10; attempts++) {
                    if (hasLock = tryAcquireMigrationLock(conf)) {
                        break;
                    }
                    AMCDB.LOGGER.info("Failed to acquire migration lock (attempt %d of 10)".formatted(attempts + 1));
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        // do nothing, no harm in going ahead and trying again
                    }
                }

                if (!hasLock) {
                    throw new RuntimeException("Could not acquire migration lock.");
                }
                dbVersion = getSchemaVersion(conf);
            }
            else {
                hasLock = true;
            }

            int targetDbVersion = MIGRATIONS.size() - 1;

            if(dbVersion > targetDbVersion) {
                throw new RuntimeException("This database has been used with a later version of AMCDB and is not compatible with this version.\n" +
                        "Database version: %d, this version of AMCDB implements database version %d.".formatted(dbVersion, MIGRATIONS.size()));
            }

            if (dbVersion == targetDbVersion) {
                AMCDB.LOGGER.info("AMCDB database is up to date.");
                return;
            }

            if(dbVersion == -1) {
                AMCDB.LOGGER.info("Installing AMCDB database...");
            }
            else {
                AMCDB.LOGGER.info("Upgrading AMCDB database from version %d to version %d...".formatted(dbVersion, targetDbVersion));
            }
            for (int i = dbVersion + 1; i <= targetDbVersion; i++) {
                Migration m;
                try {
                    m = MIGRATIONS.get(i).getDeclaredConstructor().newInstance();
                }
                catch(Exception e) {
                    throw new RuntimeException("Failed to instantiate migration %s".formatted(MIGRATIONS.get(i).getName()), e);
                }
                AMCDB.LOGGER.info("\tApplying database schema version %d: %s".formatted(i, m.describe()));
                m.apply(conf);
            }

            saveSchemaVersion(targetDbVersion, conf);
        }
        finally {
            // No matter what happens, make our best attempt to release the lock
            if(hasLock) {
                releaseMigrationLock(conf);
            }
        }
    }

    /**
     * Returns the current version of the metadata database
     * @param conf
     * @return
     */
    private static int getSchemaVersion(Configuration conf) {
        Record2<String, String> dbVersionRow = conf.dsl()
                .select(AMCDB_METADATA_KEY, AMCDB_METADATA_VALUE)
                .from(AMCDB_METADATA)
                .where(AMCDB_METADATA_KEY.eq(SCHEMA_VERSION_KEY))
                .fetchOne();

        return dbVersionRow == null ? 0 : Integer.parseInt(dbVersionRow.get(AMCDB_METADATA_VALUE), 10);
    }

    /**
     * Sets the database version to the provided value.
     * @param version
     * @param conf
     */
    private static void saveSchemaVersion(int version, Configuration conf) {
        String versionString = Integer.toString(version, 10);

        int numUpdated = conf.dsl().update(AMCDB_METADATA)
                .set(AMCDB_METADATA_VALUE, versionString)
                .where(AMCDB_METADATA_KEY.eq(SCHEMA_VERSION_KEY))
                .execute();

        if(numUpdated == 0) {
            conf.dsl().insertInto(AMCDB_METADATA)
                    .set(AMCDB_METADATA_KEY, SCHEMA_VERSION_KEY)
                    .set(AMCDB_METADATA_VALUE, versionString)
                    .execute();
        }
    }

    /**
     * Tries to acquire the migration lock by setting the migration lock metadata key to true.
     * @param conf jOOQ Configuration
     * @return True if lock was acquired; false otherwise.
     */
    private static boolean tryAcquireMigrationLock(Configuration conf) {
        return conf.dsl().transactionResult(trx -> {
            Record2<String, String> migrationLockRow = trx.dsl()
                    .select(AMCDB_METADATA_KEY, AMCDB_METADATA_VALUE)
                    .from(AMCDB_METADATA)
                    .where(AMCDB_METADATA_KEY.eq(MIGRATION_LOCK_KEY))
                    .forUpdate()
                    .fetchOne();

            // assume that if the table exists and the lock row isn't present,
            // that the initial migration is in progress
            if(migrationLockRow == null || migrationLockRow.get(AMCDB_METADATA_VALUE).equals("true")) {
                return false;
            }

            trx.dsl().update(AMCDB_METADATA)
                    .set(AMCDB_METADATA_VALUE, "true")
                    .where(AMCDB_METADATA_KEY.eq(MIGRATION_LOCK_KEY))
                    .execute();

            return true;
        });
    }

    /**
     * Releases the migration lock by setting the migration lock metadata key to false.
     * @param conf jOOQ Configuration
     */
    private static void releaseMigrationLock(Configuration conf) {
        conf.dsl().transaction(trx -> {
            int numUpdated = trx.dsl().update(AMCDB_METADATA)
                    .set(AMCDB_METADATA_VALUE, "false")
                    .where(AMCDB_METADATA_KEY.eq(MIGRATION_LOCK_KEY))
                    .execute();

            if(numUpdated == 0) {
                trx.dsl().insertInto(AMCDB_METADATA)
                        .set(AMCDB_METADATA_KEY, MIGRATION_LOCK_KEY)
                        .set(AMCDB_METADATA_VALUE, "false")
                        .execute();
            }
        });
    }

    /**
     * Returns a String describing what this migration does to the database.
     * @return
     */
    protected abstract String describe();

    /**
     * Executes the DDL for this migration.
     * @param conf
     */
    protected abstract void apply(Configuration conf);

    private static class V0_Metadata extends Migration {

        @Override
        protected String describe() {
            return "Create metadata table";
        }

        @Override
        protected void apply(Configuration conf) {
            conf.dsl().createTable(AMCDB_METADATA)
                    .column(AMCDB_METADATA_KEY, SQLDataType.VARCHAR.notNull())
                    .column(AMCDB_METADATA_VALUE, SQLDataType.VARCHAR)
                    .execute();
        }
    }
}
