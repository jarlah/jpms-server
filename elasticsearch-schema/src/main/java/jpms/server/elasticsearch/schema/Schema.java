package jpms.server.elasticsearch.schema;

import jpms.server.core.config.Config;
import jpms.server.elasticsearch.EsSchemaApplier;
import tools.jackson.databind.ObjectMapper;

public final class Schema {

    public static int apply(Config config, ObjectMapper json) {
        var indexes = new ElasticsearchSchemaProvider().indexes(json);
        EsSchemaApplier.open(config, json).upsertIndexes(indexes);
        return indexes.size();
    }

    private Schema() {}
}
