module dev.jarl.jpmsserver.app {
    requires dev.jarl.jpmsserver.core;
    requires tools.jackson.databind;

    uses dev.jarl.jpmsserver.core.db.NoteStoreProvider;
    uses dev.jarl.jpmsserver.core.search.SearchIndexProvider;
}
