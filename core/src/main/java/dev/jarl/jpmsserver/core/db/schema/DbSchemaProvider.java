package dev.jarl.jpmsserver.core.db.schema;

import java.util.List;

public interface DbSchemaProvider {

    List<DbSchema> schemas();
}
