package jpms.server.elasticsearch;

import java.util.Objects;
import tools.jackson.databind.node.ObjectNode;

public record EsIndexDefinition(String name, ObjectNode definition) {

    public EsIndexDefinition {
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
