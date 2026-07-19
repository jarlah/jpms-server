package jpms.server.core.search.schema;

import java.util.List;
import tools.jackson.databind.ObjectMapper;

public interface SearchSchemaProvider {

    List<SearchIndexDefinition> indexes(ObjectMapper json);
}
