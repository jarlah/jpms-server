module jpms.server.elasticsearch.schema {
    requires transitive jpms.server.elasticsearch;
    requires transitive tools.jackson.databind;

    exports jpms.server.elasticsearch.schema;
}
