package jpms.server.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PostgresDataSourcesTest {

    @Test
    void postgresTargetBecomesJdbcUrl() {
        assertEquals(
                "jdbc:postgresql://localhost:5432/app",
                PostgresConfig.from(Map.of(PostgresConfig.env("TARGET"), "localhost:5432/app"))
                        .jdbcUrl());
        assertEquals(
                "jdbc:postgresql://db:5432/app?sslmode=require",
                PostgresConfig.from(
                                Map.of(PostgresConfig.env("TARGET"), "db:5432/app?sslmode=require"))
                        .jdbcUrl());
    }

    @Test
    void fullPostgresJdbcUrlPassesThrough() {
        String jdbcUrl = "jdbc:postgresql://db:5432/app?sslmode=require";

        assertEquals(
                jdbcUrl,
                PostgresConfig.from(Map.of(PostgresConfig.env("TARGET"), jdbcUrl)).jdbcUrl());
    }

    @Test
    void nonPostgresJdbcPrefixIsTreatedAsTargetText() {
        assertEquals(
                "jdbc:postgresql://jdbc:h2:mem:notes",
                PostgresConfig.from(Map.of(PostgresConfig.env("TARGET"), "jdbc:h2:mem:notes"))
                        .jdbcUrl());
    }

    @Test
    void configParsesPostgresValuesWithDefaults() {
        PostgresConfig defaults = PostgresConfig.from(Map.of());
        assertEquals("jdbc:postgresql://localhost:5432/app", defaults.jdbcUrl());
        assertEquals("app", defaults.user());
        assertEquals("app", defaults.password());
        assertEquals(10, defaults.poolSize());

        PostgresConfig configured =
                PostgresConfig.from(
                        Map.of(
                                PostgresConfig.env("TARGET"), "db:5432/app",
                                PostgresConfig.env("USER"), "notes",
                                PostgresConfig.env("PASS"), "secret",
                                PostgresConfig.env("POOL_SIZE"), "4"));
        assertEquals("jdbc:postgresql://db:5432/app", configured.jdbcUrl());
        assertEquals("notes", configured.user());
        assertEquals("secret", configured.password());
        assertEquals(4, configured.poolSize());
    }
}
