package jpms.server.postgres;

import java.util.Map;
import jpms.server.core.db.schema.DbSchemaApplier;
import jpms.server.core.db.schema.DbSchemaApplierProvider;

public final class PostgresSchemaApplierProvider implements DbSchemaApplierProvider {

    @Override
    public DbSchemaApplier create(Map<String, String> config) {
        var dataSource = PostgresDataSources.pooled(PostgresConfig.from(config));
        return new PostgresSchemaApplier(dataSource, dataSource);
    }
}
