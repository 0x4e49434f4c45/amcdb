package network.parthenon.amcdb.data.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public abstract class DataService {

    protected final Connection dbConnection;

    public DataService(Connection dbConnection){
        this.dbConnection = dbConnection;
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
    protected <T> T query(Function<ResultSet, T> resultTransform, String sql, Object... params)
        throws SQLException {
        try(PreparedStatement stmt = dbConnection
                .prepareStatement(sql)) {
            for(int i = 0; i < params.length; i++) {
                // placeholders are 1-indexed
                stmt.setObject(i+1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return resultTransform.apply(rs);
            }
        }
    }

    protected int execute(String sql, Object... params) throws SQLException {
        try(PreparedStatement stmt = dbConnection
                .prepareStatement(sql)) {
            for(int i = 0; i < params.length; i++) {
                // placeholders are 1-indexed
                stmt.setObject(i+1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }
}
