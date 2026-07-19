package jpms.server.postgres;

import jpms.server.core.config.Config;

record PostgresConfig(String jdbcUrl, String user, String password, int poolSize) {

    private static final String PREFIX = "jpms.server.postgres.";

    static PostgresConfig from(Config config) {
        return new PostgresConfig(
                jdbcUrl(config.getString(path("target"), "localhost:5432/app")),
                config.getString(path("user"), "app"),
                config.getString(path("pass"), "app"),
                config.getInt(path("pool.size"), 10));
    }

    static String path(String name) {
        return PREFIX + name;
    }

    private static String jdbcUrl(String target) {
        return target.startsWith("jdbc:postgresql:") ? target : "jdbc:postgresql://" + target;
    }
}
