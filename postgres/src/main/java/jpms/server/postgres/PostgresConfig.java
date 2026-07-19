package jpms.server.postgres;

import java.util.Map;

record PostgresConfig(String jdbcUrl, String user, String password, int poolSize) {

    private static final String ENV_PREFIX = "JPMS_SERVER_POSTGRES_";

    static PostgresConfig from(Map<String, String> values) {
        return new PostgresConfig(
                jdbcUrl(values.getOrDefault(env("TARGET"), "localhost:5432/app")),
                values.getOrDefault(env("USER"), "app"),
                values.getOrDefault(env("PASS"), "app"),
                Integer.parseInt(values.getOrDefault(env("POOL_SIZE"), "10")));
    }

    static String env(String name) {
        return ENV_PREFIX + name;
    }

    private static String jdbcUrl(String target) {
        return target.startsWith("jdbc:postgresql:") ? target : "jdbc:postgresql://" + target;
    }
}
