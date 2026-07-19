package jpms.server.core.search.schema;

import java.util.Map;
import tools.jackson.databind.ObjectMapper;

public interface SearchSchemaApplierProvider {

    SearchSchemaApplier create(Map<String, String> config, ObjectMapper json);
}
