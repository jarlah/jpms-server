package dev.jarl.jpmsserver.elasticsearch;

import dev.jarl.jpmsserver.core.search.schema.SearchIndexDefinition;
import dev.jarl.jpmsserver.core.search.schema.SearchSchemaApplier;
import java.util.List;

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
