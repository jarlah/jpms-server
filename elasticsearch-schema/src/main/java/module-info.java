module jpms.server.elasticsearch.schema {
    requires transitive jpms.server.core;
    requires transitive tools.jackson.databind;

    exports jpms.server.elasticsearch.schema;

    uses jpms.server.core.search.schema.SearchSchemaApplierProvider;
    uses jpms.server.core.search.schema.SearchSchemaProvider;

    provides jpms.server.core.search.schema.SearchSchemaProvider with
            jpms.server.elasticsearch.schema.ElasticsearchSchemaProvider;
}
