package jpms.server.postgres;

import com.zaxxer.hikari.HikariDataSource;
import jpms.server.core.config.Config;
import jpms.server.core.db.NoteRepository;
import jpms.server.core.db.NoteStore;
import jpms.server.core.db.NoteStoreProvider;

public final class PostgresNoteStoreProvider implements NoteStoreProvider {

    @Override
    public NoteStore open(Config config) {
        HikariDataSource dataSource = PostgresDataSources.pooled(PostgresConfig.from(config));
        return new PostgresNoteStore(dataSource);
    }

    private record PostgresNoteStore(HikariDataSource dataSource) implements NoteStore {

        @Override
        public NoteRepository repository() {
            return new PostgresNoteRepository(dataSource);
        }

        @Override
        public void close() {
            dataSource.close();
        }
    }
}
