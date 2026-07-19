module dev.jarl.jpmsserver.postgres {
    requires transitive dev.jarl.jpmsserver.core;
    requires transitive java.sql;
    requires transitive com.zaxxer.hikari;
    requires org.postgresql.jdbc;

    exports dev.jarl.jpmsserver.postgres;

    provides dev.jarl.jpmsserver.core.db.NoteStoreProvider with
            dev.jarl.jpmsserver.postgres.PostgresNoteStoreProvider;
    provides dev.jarl.jpmsserver.core.db.schema.DbSchemaApplierProvider with
            dev.jarl.jpmsserver.postgres.PostgresSchemaApplierProvider;
}
