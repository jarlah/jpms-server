module dev.jarl.jpmsserver.core {
    requires transitive jdk.httpserver;
    requires transitive tools.jackson.databind;

    exports dev.jarl.jpmsserver.core.db;
    exports dev.jarl.jpmsserver.core.db.schema;
    exports dev.jarl.jpmsserver.core.http;
    exports dev.jarl.jpmsserver.core.notes;
    exports dev.jarl.jpmsserver.core.search;
    exports dev.jarl.jpmsserver.core.search.schema;
}
