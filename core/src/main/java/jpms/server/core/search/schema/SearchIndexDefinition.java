package jpms.server.core.search.schema;

import java.util.Objects;
import tools.jackson.databind.node.ObjectNode;

/** Portable index definition owned by the application and applied by a search backend module. */
public record SearchIndexDefinition(String name, ObjectNode definition) {

    public SearchIndexDefinition {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("index name must not be blank");
        }
        definition = definition == null ? null : definition.deepCopy();
    }

    @Override
    public ObjectNode definition() {
        return definition == null ? null : definition.deepCopy();
    }
}
