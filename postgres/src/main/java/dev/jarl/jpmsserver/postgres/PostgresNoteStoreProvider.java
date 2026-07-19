package dev.jarl.jpmsserver.postgres;

import com.zaxxer.hikari.HikariDataSource;
import dev.jarl.jpmsserver.core.db.NoteRepository;
import dev.jarl.jpmsserver.core.db.NoteStore;
import dev.jarl.jpmsserver.core.db.NoteStoreProvider;
import java.util.Map;

public final class PostgresNoteStoreProvider implements NoteStoreProvider {

    @Override
    public NoteStore open(Map<String, String> config) {
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
