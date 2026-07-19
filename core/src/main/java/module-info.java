module jpms.server.core {
    requires transitive jdk.httpserver;
    requires transitive tools.jackson.databind;

    exports jpms.server.core.db;
    exports jpms.server.core.http;
    exports jpms.server.core.notes;
    exports jpms.server.core.search;
}
