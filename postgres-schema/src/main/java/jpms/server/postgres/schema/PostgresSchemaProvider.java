package jpms.server.postgres.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import jpms.server.core.db.schema.DbSchema;
import jpms.server.core.db.schema.DbSchemaProvider;

public final class PostgresSchemaProvider implements DbSchemaProvider {

    @Override
    public List<DbSchema> schemas() {
        return List.of(new DbSchema("app", statements(readResource("/schema.sql"))));
    }

    private static List<String> statements(String ddl) {
        return Arrays.stream(ddl.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .toList();
    }

    private static String readResource(String path) {
        try (InputStream in = PostgresSchemaProvider.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
