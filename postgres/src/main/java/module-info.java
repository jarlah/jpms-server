module jpms.server.postgres {
    requires transitive jpms.server.core;
    requires transitive java.sql;
    requires transitive com.zaxxer.hikari;
    requires org.postgresql.jdbc;

    exports jpms.server.postgres;

    provides jpms.server.core.db.NoteStoreProvider with
            jpms.server.postgres.PostgresNoteStoreProvider;
    provides jpms.server.core.db.schema.DbSchemaApplierProvider with
            jpms.server.postgres.PostgresSchemaApplierProvider;
}
