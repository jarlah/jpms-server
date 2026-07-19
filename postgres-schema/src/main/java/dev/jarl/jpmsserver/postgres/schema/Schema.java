package dev.jarl.jpmsserver.postgres.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public final class Schema {

    public static void apply(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            apply(connection);
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    public static void apply(Connection connection) {
        String ddl = readResource("/schema.sql");
        try (Statement statement = connection.createStatement()) {
            for (String sql : ddl.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    private static String readResource(String path) {
        try (InputStream in = Schema.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Schema() {}

    public static final class UncheckedSqlException extends RuntimeException {

        private UncheckedSqlException(SQLException cause) {
            super(cause);
        }
    }
}
