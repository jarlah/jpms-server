package jpms.server.core.db.schema;

import java.util.List;

public interface DbSchemaProvider {

    List<DbSchema> schemas();
}
