package jpms.server.elasticsearch;

import java.util.List;
import jpms.server.core.search.schema.SearchIndexDefinition;
import jpms.server.core.search.schema.SearchSchemaApplier;

public final class EsSchemaApplier implements SearchSchemaApplier {

    private final EsRestClient client;

    public EsSchemaApplier(EsRestClient client) {
        this.client = client;
    }

    @Override
    public void upsertIndexes(List<SearchIndexDefinition> indexes) {
        for (SearchIndexDefinition index : indexes) {
            client.upsertIndex(index.name(), index.definition());
        }
    }
}
