package jpms.server.elasticsearch;

import java.util.Map;
import jpms.server.core.search.schema.SearchSchemaApplier;
import jpms.server.core.search.schema.SearchSchemaApplierProvider;
import tools.jackson.databind.ObjectMapper;

public final class EsSchemaApplierProvider implements SearchSchemaApplierProvider {

    @Override
    public SearchSchemaApplier create(Map<String, String> config, ObjectMapper json) {
        ElasticsearchConfig elasticsearch = ElasticsearchConfig.from(config);
        return new EsSchemaApplier(
                new EsRestClient(elasticsearch.base(), elasticsearch.authHeader(), json));
    }
}
