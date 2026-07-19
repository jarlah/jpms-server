package jpms.server.elasticsearch;

import jpms.server.core.config.Config;
import jpms.server.core.search.SearchIndex;
import jpms.server.core.search.SearchIndexProvider;
import tools.jackson.databind.ObjectMapper;

public final class EsRestClientProvider implements SearchIndexProvider {

    @Override
    public SearchIndex create(Config config, ObjectMapper json) {
        ElasticsearchConfig elasticsearch = ElasticsearchConfig.from(config);
        return new EsRestClient(elasticsearch.base(), elasticsearch.authHeader(), json);
    }
}
