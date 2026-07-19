package dev.jarl.jpmsserver.elasticsearch;

import dev.jarl.jpmsserver.core.search.SearchIndex;
import dev.jarl.jpmsserver.core.search.SearchIndexProvider;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

public final class EsRestClientProvider implements SearchIndexProvider {

    @Override
    public SearchIndex create(Map<String, String> config, ObjectMapper json) {
        ElasticsearchConfig elasticsearch = ElasticsearchConfig.from(config);
        return new EsRestClient(elasticsearch.base(), elasticsearch.authHeader(), json);
    }
}
