package jpms.server.postgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import jpms.server.core.config.Config;

public final class PostgresSchemaApplier implements AutoCloseable {

    private final DataSource dataSource;
    private final AutoCloseable closeable;

    public static PostgresSchemaApplier open(Config config) {
        var dataSource = PostgresDataSources.pooled(PostgresConfig.from(config));
        return new PostgresSchemaApplier(dataSource, dataSource);
    }

    public PostgresSchemaApplier(DataSource dataSource, AutoCloseable closeable) {
        this.dataSource = dataSource;
        this.closeable = closeable;
    }

    public void apply(List<DbSchema> schemas) {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            for (DbSchema schema : schemas) {
                for (String sql : schema.statements()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    @Override
    public void close() {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
