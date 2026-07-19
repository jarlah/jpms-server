package dev.jarl.jpmsserver.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariDataSource;
import dev.jarl.jpmsserver.core.notes.Note;
import dev.jarl.jpmsserver.postgres.schema.Schema;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresNoteRepositoryTest {

    private static HikariDataSource dataSource;
    private PostgresNoteRepository repository;

    @BeforeAll
    static void createDatabase() {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(
                "jdbc:h2:mem:notes_pg;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        Schema.apply(dataSource);
    }

    @AfterAll
    static void closeDatabase() {
        dataSource.close();
    }

    @BeforeEach
    void resetState() throws SQLException {
        try (var connection = dataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("DELETE FROM notes");
        }
        repository = new PostgresNoteRepository(dataSource);
    }

    @Test
    void insertsFindsListsAndDeletesNotes() {
        Note first = repository.insert("One", "first note");
        Note second = repository.insert("Two", "second note");

        assertEquals("One", repository.findById(first.id()).orElseThrow().title());
        assertEquals(2, repository.findAll().size());
        assertEquals(first.id(), repository.findAll().get(0).id());
        assertEquals(second.id(), repository.findAll().get(1).id());

        assertTrue(repository.delete(first.id()));
        assertTrue(repository.findById(first.id()).isEmpty());
        assertFalse(repository.delete(first.id()));
    }
}
