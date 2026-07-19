package jpms.server.core.config;

public final class ConfigException extends RuntimeException {

    private ConfigException(String message) {
        super(message);
    }

    static ConfigException missing(String path) {
        return new ConfigException("No configuration setting found for key '" + path + "'");
    }

    static ConfigException badValue(String path, String type, String value) {
        return new ConfigException(
                "Configuration key '"
                        + path
                        + "' has value '"
                        + value
                        + "' which is not a valid "
                        + type);
    }
}
