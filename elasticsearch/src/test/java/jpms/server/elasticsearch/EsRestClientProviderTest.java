package jpms.server.elasticsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import jpms.server.core.config.Config;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EsRestClientProviderTest {

    @Test
    void configParsesSearchValuesWithDefaults() {
        ElasticsearchConfig defaults = ElasticsearchConfig.from(Config.of(Map.of()));
        assertEquals("http://localhost:9200", defaults.base().toString());

        ElasticsearchConfig configured =
                ElasticsearchConfig.from(
                        Config.of(
                                Map.of(
                                        ElasticsearchConfig.path("target"),
                                        "https://search.example.com",
                                        ElasticsearchConfig.path("api.key"),
                                        "secret123")));
        assertEquals("https://search.example.com", configured.base().toString());
        assertEquals("ApiKey secret123", configured.authHeader());
    }

    @Test
    void apiKeyAuthIsOwnedByElasticsearchProvider() throws Exception {
        try (var stub = new StubEsServer()) {
            stub.enqueue(
                    StubEsServer.Response.of(
                            200,
                            """
                    {"count":1}"""));

            var search =
                    new EsRestClientProvider()
                            .create(
                                    Config.of(
                                            Map.of(
                                                    ElasticsearchConfig.path("target"),
                                                    stub.uri().toString(),
                                                    ElasticsearchConfig.path("api.key"),
                                                    "secret123")),
                                    new JsonMapper());

            search.count("notes", null);

            assertEquals("ApiKey secret123", stub.requests().get(0).header("Authorization"));
        }
    }

    @Test
    void basicAuthIsOwnedByElasticsearchProvider() throws Exception {
        try (var stub = new StubEsServer()) {
            stub.enqueue(
                    StubEsServer.Response.of(
                            200,
                            """
                    {"count":1}"""));

            var search =
                    new EsRestClientProvider()
                            .create(
                                    Config.of(
                                            Map.of(
                                                    ElasticsearchConfig.path("target"),
                                                    stub.uri().toString(),
                                                    ElasticsearchConfig.path("user"),
                                                    "elastic",
                                                    ElasticsearchConfig.path("pass"),
                                                    "secret")),
                                    new JsonMapper());

            search.count("notes", null);

            assertEquals(
                    "Basic ZWxhc3RpYzpzZWNyZXQ=", stub.requests().get(0).header("Authorization"));
        }
    }
}
