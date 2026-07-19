package jpms.server.elasticsearch.schema;

import java.util.Map;
import jpms.server.elasticsearch.EsSchemaApplier;
import tools.jackson.databind.ObjectMapper;

public final class Schema {

    public static int apply(Map<String, String> config, ObjectMapper json) {
        var indexes = new ElasticsearchSchemaProvider().indexes(json);
        EsSchemaApplier.open(config, json).upsertIndexes(indexes);
        return indexes.size();
    }

    private Schema() {}
}
