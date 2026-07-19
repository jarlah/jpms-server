module jpms.server.elasticsearch {
    requires transitive jpms.server.core;
    requires transitive java.net.http;
    requires transitive tools.jackson.databind;
    requires jdk.httpserver;

    exports jpms.server.elasticsearch;

    provides jpms.server.core.search.SearchIndexProvider with
            jpms.server.elasticsearch.EsRestClientProvider;
}
