module dev.jarl.jpmsserver.elasticsearch.schema {
    requires transitive dev.jarl.jpmsserver.core;
    requires transitive tools.jackson.databind;

    exports dev.jarl.jpmsserver.elasticsearch.schema;

    uses dev.jarl.jpmsserver.core.search.schema.SearchSchemaApplierProvider;
    uses dev.jarl.jpmsserver.core.search.schema.SearchSchemaProvider;

    provides dev.jarl.jpmsserver.core.search.schema.SearchSchemaProvider with
            dev.jarl.jpmsserver.elasticsearch.schema.ElasticsearchSchemaProvider;
}
