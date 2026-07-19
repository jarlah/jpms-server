package dev.jarl.jpmsserver.core.db.schema;

import java.util.List;
import java.util.Objects;

public record DbSchema(String name, List<String> statements) {

    public DbSchema {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("schema name must not be blank");
        }
        statements = List.copyOf(Objects.requireNonNull(statements, "statements"));
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("schema statements must not be empty");
        }
    }
}
