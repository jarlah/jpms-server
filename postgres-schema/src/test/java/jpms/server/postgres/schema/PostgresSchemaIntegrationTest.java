package jpms.server.postgres.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import jpms.server.core.notes.Note;
import jpms.server.postgres.PostgresNoteStoreProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PostgresSchemaIntegrationTest {

    @Container
    @SuppressWarnings("resource") // Closed by the Testcontainers JUnit extension.
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("app")
                    .withUsername("app")
                    .withPassword("app");

    @BeforeAll
    static void applyProductionSchema() {
        assertEquals(1, Schema.apply(config()));
        assertEquals(1, Schema.apply(config()), "schema application must be idempotent");
    }

    @Test
    void productionSchemaSupportsTheNoteRepository() {
        try (var store = new PostgresNoteStoreProvider().open(config())) {
            var repository = store.repository();

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

    private static Map<String, String> config() {
        return Map.of(
                "JPMS_SERVER_POSTGRES_TARGET", POSTGRES.getJdbcUrl(),
                "JPMS_SERVER_POSTGRES_USER", POSTGRES.getUsername(),
                "JPMS_SERVER_POSTGRES_PASS", POSTGRES.getPassword(),
                "JPMS_SERVER_POSTGRES_POOL_SIZE", "2");
    }
}
