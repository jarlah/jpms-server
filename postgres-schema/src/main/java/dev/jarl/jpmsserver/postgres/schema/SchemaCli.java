package dev.jarl.jpmsserver.postgres.schema;

import java.sql.DriverManager;
import java.util.Map;

public final class SchemaCli {

    public static void main(String[] args) throws Exception {
        Map<String, String> env = System.getenv();
        String target =
                env.getOrDefault(
                        "JPMS_SERVER_POSTGRES_TARGET",
                        env.getOrDefault(
                                "STORE_TARGET",
                                env.getOrDefault(
                                        "DB_TARGET",
                                        env.getOrDefault("JDBC_URL", "localhost:5432/app"))));
        String jdbcUrl =
                target.startsWith("jdbc:postgresql:") ? target : "jdbc:postgresql://" + target;
        String user =
                env.getOrDefault(
                        "JPMS_SERVER_POSTGRES_USER",
                        env.getOrDefault("STORE_USER", env.getOrDefault("DB_USER", "app")));
        String password =
                env.getOrDefault(
                        "JPMS_SERVER_POSTGRES_PASS",
                        env.getOrDefault("STORE_PASS", env.getOrDefault("DB_PASS", "app")));

        try (var connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            Schema.apply(connection);
        }
        System.out.println("applied schema to " + jdbcUrl);
    }

    private SchemaCli() {}
}
