package jpms.server.postgres.schema;

import java.util.Map;
import jpms.server.postgres.PostgresSchemaApplier;

public final class Schema {

    public static int apply(Map<String, String> config) {
        var schemas = new PostgresSchemaProvider().schemas();
        try (var applier = PostgresSchemaApplier.open(config)) {
            applier.apply(schemas);
        }
        return schemas.stream().mapToInt(schema -> schema.statements().size()).sum();
    }

    private Schema() {}
}
