package jpms.server.core.search;

import jpms.server.core.config.Config;
import tools.jackson.databind.ObjectMapper;

public interface SearchIndexProvider {

    SearchIndex create(Config config, ObjectMapper json);
}
