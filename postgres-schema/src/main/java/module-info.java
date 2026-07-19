module dev.jarl.jpmsserver.postgres.schema {
    requires transitive dev.jarl.jpmsserver.core;

    exports dev.jarl.jpmsserver.postgres.schema;

    uses dev.jarl.jpmsserver.core.db.schema.DbSchemaApplierProvider;
    uses dev.jarl.jpmsserver.core.db.schema.DbSchemaProvider;

    provides dev.jarl.jpmsserver.core.db.schema.DbSchemaProvider with
            dev.jarl.jpmsserver.postgres.schema.PostgresSchemaProvider;
}
