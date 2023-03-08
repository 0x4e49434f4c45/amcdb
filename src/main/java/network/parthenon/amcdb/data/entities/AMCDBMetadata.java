package network.parthenon.amcdb.data.entities;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

import java.beans.ConstructorProperties;

import static org.jooq.impl.DSL.*;

public class AMCDBMetadata {

    public static final Table<Record> TABLE = table(name("amcdb_metadata"));

    private static final String KEY_COLUMN = "key";
    public static final Field<String> KEY = field(name(KEY_COLUMN), SQLDataType.VARCHAR);
    private static final String VALUE_COLUMN = "value";
    public static final Field<String> VALUE = field(name(VALUE_COLUMN), SQLDataType.VARCHAR);

    public static final String SCHEMA_VERSION = "schema_version";

    public static final String MIGRATION_LOCK = "migration_lock";

    private String key;

    private String value;

    /**
     * Creates a new AMCDBMetadata entry.
     * @param key   Metadata key
     * @param value Metadata value
     */
    @ConstructorProperties({KEY_COLUMN, VALUE_COLUMN})
    public AMCDBMetadata(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
