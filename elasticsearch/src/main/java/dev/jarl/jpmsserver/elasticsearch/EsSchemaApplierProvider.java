package dev.jarl.jpmsserver.elasticsearch;

import dev.jarl.jpmsserver.core.search.schema.SearchSchemaApplier;
import dev.jarl.jpmsserver.core.search.schema.SearchSchemaApplierProvider;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

public final class EsSchemaApplierProvider implements SearchSchemaApplierProvider {

    @Override
    public SearchSchemaApplier create(Map<String, String> config, ObjectMapper json) {
        ElasticsearchConfig elasticsearch = ElasticsearchConfig.from(config);
        return new EsSchemaApplier(
                new EsRestClient(elasticsearch.base(), elasticsearch.authHeader(), json));
    }
}
