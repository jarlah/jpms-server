package jpms.server.postgres.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import jpms.server.core.db.schema.DbSchema;
import jpms.server.core.db.schema.DbSchemaApplierProvider;
import jpms.server.core.db.schema.DbSchemaProvider;

public final class Schema {

    public static int apply(Map<String, String> config) {
        List<DbSchema> schemas = schemas();
        try (var applier = provider(DbSchemaApplierProvider.class).create(config)) {
            applier.apply(schemas);
        }
        return schemas.stream().mapToInt(schema -> schema.statements().size()).sum();
    }

    private static List<DbSchema> schemas() {
        Map<String, DbSchema> schemas = new LinkedHashMap<>();
        for (DbSchemaProvider provider : ServiceLoader.load(DbSchemaProvider.class)) {
            for (DbSchema schema : provider.schemas()) {
                DbSchema previous = schemas.putIfAbsent(schema.name(), schema);
                if (previous != null) {
                    throw new IllegalStateException(
                            "duplicate db schema definition: " + schema.name());
                }
            }
        }
        if (schemas.isEmpty()) {
            throw new IllegalStateException(
                    "no provider found for " + DbSchemaProvider.class.getName());
        }
        return List.copyOf(schemas.values());
    }

    private static <T> T provider(Class<T> type) {
        var providers = ServiceLoader.load(type).stream().map(ServiceLoader.Provider::get).toList();
        if (providers.isEmpty()) {
            throw new IllegalStateException("no provider found for " + type.getName());
        }
        if (providers.size() > 1) {
            throw new IllegalStateException(
                    "multiple providers found for " + type.getName() + ": " + providers);
        }
        return providers.getFirst();
    }

    private Schema() {}
}
