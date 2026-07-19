package dev.jarl.jpmsserver.core.db.schema;

import java.util.List;

public interface DbSchemaApplier extends AutoCloseable {

    void apply(List<DbSchema> schemas);

    @Override
    default void close() {}
}
