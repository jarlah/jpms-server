package dev.jarl.jpmsserver.postgres;

import dev.jarl.jpmsserver.core.db.schema.DbSchemaApplier;
import dev.jarl.jpmsserver.core.db.schema.DbSchemaApplierProvider;
import java.util.Map;

public final class PostgresSchemaApplierProvider implements DbSchemaApplierProvider {

    @Override
    public DbSchemaApplier create(Map<String, String> config) {
        var dataSource = PostgresDataSources.pooled(PostgresConfig.from(config));
        return new PostgresSchemaApplier(dataSource, dataSource);
    }
}
