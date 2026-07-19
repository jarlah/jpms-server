package jpms.server.elasticsearch;

import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

public final class EsSchemaApplier {

    private final EsRestClient client;

    public static EsSchemaApplier open(Map<String, String> config, ObjectMapper json) {
        ElasticsearchConfig elasticsearch = ElasticsearchConfig.from(config);
        return new EsSchemaApplier(
                new EsRestClient(elasticsearch.base(), elasticsearch.authHeader(), json));
    }

    public EsSchemaApplier(EsRestClient client) {
        this.client = client;
    }

    public void upsertIndexes(List<EsIndexDefinition> indexes) {
        for (EsIndexDefinition index : indexes) {
            client.upsertIndex(index.name(), index.definition());
        }
    }
}
