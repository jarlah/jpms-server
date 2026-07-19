module jpms.server.app {
    requires jpms.server.core;
    requires tools.jackson.databind;

    uses jpms.server.core.db.NoteStoreProvider;
    uses jpms.server.core.search.SearchIndexProvider;
}
