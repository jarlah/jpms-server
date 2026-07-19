package jpms.server.core.db.schema;

import java.util.Map;

public interface DbSchemaApplierProvider {

    DbSchemaApplier create(Map<String, String> config);
}
