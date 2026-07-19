package jpms.server.postgres.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import jpms.server.postgres.DbSchema;

public final class PostgresSchemaProvider {

    public List<DbSchema> schemas() {
        return List.of(new DbSchema("app", statements(readSchema())));
    }

    private static List<String> statements(String ddl) {
        return Arrays.stream(ddl.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .toList();
    }

    private static String readSchema() {
        try (InputStream in = PostgresSchemaProvider.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + "/schema.sql");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
