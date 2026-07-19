module dev.jarl.jpmsserver.elasticsearch {
    requires transitive dev.jarl.jpmsserver.core;
    requires transitive java.net.http;
    requires transitive tools.jackson.databind;
    requires jdk.httpserver;

    exports dev.jarl.jpmsserver.elasticsearch;

    provides dev.jarl.jpmsserver.core.search.SearchIndexProvider with
            dev.jarl.jpmsserver.elasticsearch.EsRestClientProvider;
    provides dev.jarl.jpmsserver.core.search.schema.SearchSchemaApplierProvider with
            dev.jarl.jpmsserver.elasticsearch.EsSchemaApplierProvider;
}
