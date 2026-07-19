module jpms.server.postgres.schema {
    requires transitive jpms.server.core;

    exports jpms.server.postgres.schema;

    uses jpms.server.core.db.schema.DbSchemaApplierProvider;
    uses jpms.server.core.db.schema.DbSchemaProvider;

    provides jpms.server.core.db.schema.DbSchemaProvider with
            jpms.server.postgres.schema.PostgresSchemaProvider;
}
