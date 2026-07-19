package jpms.server;

import java.util.ServiceLoader;
import jpms.server.core.db.NoteStoreProvider;
import jpms.server.core.http.HttpApi;
import jpms.server.core.notes.NoteService;
import jpms.server.core.search.SearchIndexProvider;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class Main {

    public static void main(String[] args) throws Exception {
        var env = System.getenv();
        Config config = Config.fromEnv(env);
        ObjectMapper json = new JsonMapper();

        var store = provider(NoteStoreProvider.class).open(env);
        try {
            var searchIndex = provider(SearchIndexProvider.class).create(env, json);
            var notes = new NoteService(store.repository(), searchIndex, json);
            notes.ensureSearchIndex();

            var server = HttpApi.start(config.port(), notes, json);
            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread(
                                    () -> {
                                        server.stop(1);
                                        store.close();
                                    }));
            System.out.println("jpms-server listening on :" + server.getAddress().getPort());
        } catch (Exception e) {
            store.close();
            throw e;
        }
    }

    private static <T> T provider(Class<T> type) {
        var providers = ServiceLoader.load(type).stream().map(ServiceLoader.Provider::get).toList();
        if (providers.isEmpty()) {
            throw new IllegalStateException("no provider found for " + type.getName());
        }
        if (providers.size() > 1) {
            throw new IllegalStateException(
                    "multiple providers found for " + type.getName() + ": " + providers);
        }
        return providers.getFirst();
    }

    private Main() {}
}
