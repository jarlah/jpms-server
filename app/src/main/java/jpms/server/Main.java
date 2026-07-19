package jpms.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ServiceLoader;
import jpms.server.core.config.Config;
import jpms.server.core.db.NoteStoreProvider;
import jpms.server.core.http.HttpApi;
import jpms.server.core.notes.NoteService;
import jpms.server.core.search.SearchIndexProvider;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class Main {

    static void main(String[] args) throws Exception {
        Config config = loadConfig();
        AppConfig appConfig = AppConfig.from(config);
        ObjectMapper json = new JsonMapper();

        var store = provider(NoteStoreProvider.class).open(config);
        try {
            var searchIndex = provider(SearchIndexProvider.class).create(config, json);
            var notes = new NoteService(store.repository(), searchIndex, json);
            notes.ensureSearchIndex();

            var server = HttpApi.start(appConfig.port(), notes, json);
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

    private static Config loadConfig() {
        try (InputStream in = Main.class.getResourceAsStream("/application.properties")) {
            if (in == null) {
                throw new IllegalStateException("application.properties not found on classpath");
            }
            return Config.load(in).withEnvOverrides(System.getenv());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
