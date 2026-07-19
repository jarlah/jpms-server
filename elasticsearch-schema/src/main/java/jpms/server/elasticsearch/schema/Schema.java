package jpms.server.elasticsearch.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import jpms.server.core.search.schema.SearchIndexDefinition;
import jpms.server.core.search.schema.SearchSchemaApplierProvider;
import jpms.server.core.search.schema.SearchSchemaProvider;
import tools.jackson.databind.ObjectMapper;

public final class Schema {

    public static int apply(Map<String, String> config, ObjectMapper json) {
        List<SearchIndexDefinition> indexes = indexes(json);
        provider(SearchSchemaApplierProvider.class).create(config, json).upsertIndexes(indexes);
        return indexes.size();
    }

    private static List<SearchIndexDefinition> indexes(ObjectMapper json) {
        Map<String, SearchIndexDefinition> indexes = new LinkedHashMap<>();
        for (SearchSchemaProvider provider : ServiceLoader.load(SearchSchemaProvider.class)) {
            for (SearchIndexDefinition index : provider.indexes(json)) {
                SearchIndexDefinition previous = indexes.putIfAbsent(index.name(), index);
                if (previous != null) {
                    throw new IllegalStateException(
                            "duplicate search index definition: " + index.name());
                }
            }
        }
        if (indexes.isEmpty()) {
            throw new IllegalStateException(
                    "no provider found for " + SearchSchemaProvider.class.getName());
        }
        return List.copyOf(indexes.values());
    }

    private static <T> T provider(Class<T> type) {
        var providers = ServiceLoader.load(type).stream().map(ServiceLoader.Provider::get).toList();
        if (providers.isEmpty()) {
            throw new IllegalStateException("no provider found for " + type.getName());
        }
        if (providers.size() > 1) {
            throw new IllegalStateException(
                    "multiple providers found for " + type.getName() + ": " + providers);
        }
        return providers.getFirst();
    }

    private Schema() {}
}
