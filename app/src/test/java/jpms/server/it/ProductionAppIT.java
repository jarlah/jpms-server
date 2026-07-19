package jpms.server.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Run with {@code mvn verify -pl app -am -Dit.test=ProductionAppIT}. Do not run this test directly
 * from an IDE: the Maven package and Failsafe phases assemble {@code target/modules} and provide
 * the {@code app.classes} and {@code app.modules} system properties used to launch the production
 * module.
 */
@Testcontainers
class ProductionAppIT {

    private static final ObjectMapper JSON = new JsonMapper();
    private static final Pattern LISTENING = Pattern.compile("jpms-server listening on :(\\d+)");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("app")
                    .withUsername("app")
                    .withPassword("app");

    @Container
    private static final ElasticsearchContainer ELASTICSEARCH =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.4.2")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @BeforeAll
    static void applyProductionSchemas() {
        jpms.server.postgres.schema.Schema.apply(config());
        jpms.server.elasticsearch.schema.Schema.apply(config(), JSON);
    }

    @Test
    void productionModuleLaunchSupportsTheHttpLifecycle(@TempDir Path tempDir) throws Exception {
        Path log = tempDir.resolve("app.log");
        Process app = startProductionApp(log);
        try {
            URI base = awaitStarted(app, log);
            assertEquals(200, request(base, "GET", "/healthz", null).status());

            String marker = "integration" + Long.toHexString(System.nanoTime());
            var createBody = JSON.createObjectNode();
            createBody.put("title", "Production module launch");
            createBody.put("body", "search marker " + marker);
            Response created = request(base, "POST", "/notes", JSON.writeValueAsString(createBody));
            assertEquals(201, created.status(), created.body());
            long id = JSON.readTree(created.body()).path("id").asLong();
            assertTrue(id > 0);

            Response found = request(base, "GET", "/notes/" + id, null);
            assertEquals(200, found.status(), found.body());
            assertEquals(
                    "Production module launch",
                    JSON.readTree(found.body()).path("title").asString());

            Response listed = request(base, "GET", "/notes", null);
            assertEquals(200, listed.status(), listed.body());
            assertEquals(1, JSON.readTree(listed.body()).size());

            awaitSearchTotal(base, marker, 1);

            Response reindexed = request(base, "POST", "/notes/_reindex", null);
            assertEquals(200, reindexed.status(), reindexed.body());
            assertFalse(JSON.readTree(reindexed.body()).path("errors").asBoolean());

            assertEquals(204, request(base, "DELETE", "/notes/" + id, null).status());
            assertEquals(404, request(base, "GET", "/notes/" + id, null).status());
            awaitSearchTotal(base, marker, 0);
        } finally {
            stop(app);
        }
    }

    private static Process startProductionApp(Path log) throws Exception {
        String java =
                Path.of(
                                System.getProperty("java.home"),
                                "bin",
                                System.getProperty("os.name").startsWith("Windows")
                                        ? "java.exe"
                                        : "java")
                        .toString();
        String modulePath =
                System.getProperty("app.classes")
                        + File.pathSeparator
                        + System.getProperty("app.modules");
        ProcessBuilder builder =
                new ProcessBuilder(
                                java,
                                "--module-path",
                                modulePath,
                                "-m",
                                "jpms.server.app/jpms.server.Main")
                        .redirectErrorStream(true)
                        .redirectOutput(log.toFile());
        builder.environment().putAll(config());
        return builder.start();
    }

    private static URI awaitStarted(Process app, Path log) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            String output = Files.exists(log) ? Files.readString(log) : "";
            var matcher = LISTENING.matcher(output);
            if (matcher.find()) {
                return URI.create("http://127.0.0.1:" + matcher.group(1) + "/");
            }
            if (!app.isAlive()) {
                fail("production app exited with " + app.exitValue() + ":\n" + output);
            }
            Thread.sleep(100);
        }
        fail("production app did not start within 30 seconds:\n" + Files.readString(log));
        return null;
    }

    private static void awaitSearchTotal(URI base, String query, long expected) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        Response last = null;
        while (System.nanoTime() < deadline) {
            last =
                    request(
                            base,
                            "GET",
                            "/notes/_search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8),
                            null);
            if (last.status() == 200
                    && JSON.readTree(last.body()).path("total").asLong() == expected) {
                return;
            }
            Thread.sleep(250);
        }
        fail("search did not reach total " + expected + "; last response: " + last);
    }

    private static Response request(URI base, String method, String path, String body)
            throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) base.resolve(path).toURL().openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("Content-Type", "application/json");
            if (body != null) {
                connection.setDoOutput(true);
                try (var out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int status = connection.getResponseCode();
            var stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody;
            if (stream == null) {
                responseBody = "";
            } else {
                try (stream) {
                    responseBody = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            return new Response(status, responseBody);
        } finally {
            connection.disconnect();
        }
    }

    private static void stop(Process app) throws InterruptedException {
        app.destroy();
        if (!app.waitFor(5, TimeUnit.SECONDS)) {
            app.destroyForcibly();
            app.waitFor(5, TimeUnit.SECONDS);
        }
    }

    private static Map<String, String> config() {
        return Map.of(
                "JPMS_SERVER_APP_PORT",
                "0",
                "JPMS_SERVER_POSTGRES_TARGET",
                POSTGRES.getJdbcUrl(),
                "JPMS_SERVER_POSTGRES_USER",
                POSTGRES.getUsername(),
                "JPMS_SERVER_POSTGRES_PASS",
                POSTGRES.getPassword(),
                "JPMS_SERVER_POSTGRES_POOL_SIZE",
                "2",
                "JPMS_SERVER_ELASTICSEARCH_TARGET",
                "http://" + ELASTICSEARCH.getHttpHostAddress());
    }

    private record Response(int status, String body) {}
}
