package dev.jarl.jpmsserver.core.search.schema;

import java.util.List;

public interface SearchSchemaApplier {

    void upsertIndexes(List<SearchIndexDefinition> indexes);
}
