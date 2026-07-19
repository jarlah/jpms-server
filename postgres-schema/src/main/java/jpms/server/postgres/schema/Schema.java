package jpms.server.postgres.schema;

import jpms.server.core.config.Config;
import jpms.server.postgres.PostgresSchemaApplier;

public final class Schema {

    public static int apply(Config config) {
        var schemas = new PostgresSchemaProvider().schemas();
        try (var applier = PostgresSchemaApplier.open(config)) {
            applier.apply(schemas);
        }
        return schemas.stream().mapToInt(schema -> schema.statements().size()).sum();
    }

    private Schema() {}
}
