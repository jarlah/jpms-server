package jpms.server.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {

    @TempDir Path tmp;

    @Test
    void readsStringAndIntFromValues() {
        Config config = Config.of(Map.of("db.host", "localhost", "db.port", "5432"));

        assertEquals("localhost", config.getString("db.host"));
        assertEquals(5432, config.getInt("db.port"));
    }

    @Test
    void loadsFromPropertiesFile() throws IOException {
        Path file = tmp.resolve("application.properties");
        Files.writeString(file, "db.host=localhost\ndb.port=5432\n");

        Config config = Config.load(file);

        assertEquals("localhost", config.getString("db.host"));
        assertEquals(5432, config.getInt("db.port"));
    }

    @Test
    void fallbackIsUsedWhenKeyIsAbsent() {
        Config config = Config.of(Map.of());

        assertEquals("default", config.getString("missing", "default"));
        assertEquals(10, config.getInt("missing", 10));
    }

    @Test
    void missingRequiredKeyThrows() {
        Config config = Config.of(Map.of());

        assertThrows(ConfigException.class, () -> config.getString("missing"));
        assertThrows(ConfigException.class, () -> config.getInt("missing"));
    }

    @Test
    void nonNumericValueThrowsOnGetInt() {
        Config config = Config.of(Map.of("db.port", "not-a-number"));

        assertThrows(ConfigException.class, () -> config.getInt("db.port"));
    }

    @Test
    void envOverrideWinsOverConfiguredValue() {
        Config config =
                Config.of(Map.of("db.host", "localhost"))
                        .withEnvOverrides(Map.of("DB_HOST", "prod-db"));

        assertEquals("prod-db", config.getString("db.host"));
    }

    @Test
    void envOverrideAppliesEvenWhenKeyIsAbsentFromValues() {
        Config config = Config.of(Map.of()).withEnvOverrides(Map.of("DB_PORT", "5555"));

        assertEquals(5555, config.getInt("db.port"));
    }

    @Test
    void envOverrideIsIgnoredWhenNotSet() {
        Config config =
                Config.of(Map.of("db.host", "localhost")).withEnvOverrides(Map.of("OTHER", "x"));

        assertEquals("localhost", config.getString("db.host"));
    }
}
