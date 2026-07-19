package jpms.server.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import jpms.server.core.config.Config;
import org.junit.jupiter.api.Test;

class PostgresDataSourcesTest {

    @Test
    void postgresTargetBecomesJdbcUrl() {
        assertEquals(
                "jdbc:postgresql://localhost:5432/app",
                PostgresConfig.from(
                                Config.of(
                                        Map.of(
                                                PostgresConfig.path("target"),
                                                "localhost:5432/app")))
                        .jdbcUrl());
        assertEquals(
                "jdbc:postgresql://db:5432/app?sslmode=require",
                PostgresConfig.from(
                                Config.of(
                                        Map.of(
                                                PostgresConfig.path("target"),
                                                "db:5432/app?sslmode=require")))
                        .jdbcUrl());
    }

    @Test
    void fullPostgresJdbcUrlPassesThrough() {
        String jdbcUrl = "jdbc:postgresql://db:5432/app?sslmode=require";

        assertEquals(
                jdbcUrl,
                PostgresConfig.from(Config.of(Map.of(PostgresConfig.path("target"), jdbcUrl)))
                        .jdbcUrl());
    }

    @Test
    void nonPostgresJdbcPrefixIsTreatedAsTargetText() {
        assertEquals(
                "jdbc:postgresql://jdbc:h2:mem:notes",
                PostgresConfig.from(
                                Config.of(
                                        Map.of(PostgresConfig.path("target"), "jdbc:h2:mem:notes")))
                        .jdbcUrl());
    }

    @Test
    void configParsesPostgresValuesWithDefaults() {
        PostgresConfig defaults = PostgresConfig.from(Config.of(Map.of()));
        assertEquals("jdbc:postgresql://localhost:5432/app", defaults.jdbcUrl());
        assertEquals("app", defaults.user());
        assertEquals("app", defaults.password());
        assertEquals(10, defaults.poolSize());

        PostgresConfig configured =
                PostgresConfig.from(
                        Config.of(
                                Map.of(
                                        PostgresConfig.path("target"), "db:5432/app",
                                        PostgresConfig.path("user"), "notes",
                                        PostgresConfig.path("pass"), "secret",
                                        PostgresConfig.path("pool.size"), "4")));
        assertEquals("jdbc:postgresql://db:5432/app", configured.jdbcUrl());
        assertEquals("notes", configured.user());
        assertEquals("secret", configured.password());
        assertEquals(4, configured.poolSize());
    }
}
