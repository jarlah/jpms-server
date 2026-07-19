package jpms.server.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class Config {

    private final Map<String, String> values;
    private final Map<String, String> env;

    private Config(Map<String, String> values, Map<String, String> env) {
        this.values = values;
        this.env = env;
    }

    public static Config load(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Config load(InputStream in) {
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return of(properties);
    }

    public static Config of(Properties properties) {
        Map<String, String> values = HashMap.newHashMap(properties.size());
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        return of(values);
    }

    public static Config of(Map<String, String> values) {
        return new Config(Map.copyOf(values), Map.of());
    }

    /** Returns a copy of this config where each key's value can be overridden by an env var. */
    public Config withEnvOverrides(Map<String, String> env) {
        return new Config(values, Map.copyOf(env));
    }

    public String getString(String path) {
        String value = resolve(path);
        if (value == null) {
            throw ConfigException.missing(path);
        }
        return value;
    }

    public String getString(String path, String fallback) {
        String value = resolve(path);
        return value != null ? value : fallback;
    }

    public int getInt(String path) {
        return parseInt(path, getString(path));
    }

    public int getInt(String path, int fallback) {
        String value = resolve(path);
        return value != null ? parseInt(path, value) : fallback;
    }

    private String resolve(String path) {
        String envValue = env.get(envKey(path));
        return envValue != null ? envValue : values.get(path);
    }

    // "jpms.server.app.port" <-> "JPMS_SERVER_APP_PORT"
    private static String envKey(String path) {
        return path.toUpperCase(Locale.ROOT).replace('.', '_');
    }

    private static int parseInt(String path, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ConfigException.badValue(path, "int", value);
        }
    }
}
