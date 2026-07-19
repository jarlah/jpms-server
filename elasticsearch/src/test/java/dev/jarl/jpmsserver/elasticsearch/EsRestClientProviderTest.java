package dev.jarl.jpmsserver.elasticsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EsRestClientProviderTest {

    @Test
    void configParsesSearchValuesWithDefaults() {
        ElasticsearchConfig defaults = ElasticsearchConfig.from(Map.of());
        assertEquals("http://localhost:9200", defaults.base().toString());

        ElasticsearchConfig configured =
                ElasticsearchConfig.from(
                        Map.of(
                                ElasticsearchConfig.env("TARGET"), "https://search.example.com",
                                ElasticsearchConfig.env("API_KEY"), "secret123"));
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
                                    Map.of(
                                            ElasticsearchConfig.env("TARGET"),
                                            stub.uri().toString(),
                                            ElasticsearchConfig.env("API_KEY"),
                                            "secret123"),
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
                                    Map.of(
                                            ElasticsearchConfig.env("TARGET"),
                                            stub.uri().toString(),
                                            ElasticsearchConfig.env("USER"),
                                            "elastic",
                                            ElasticsearchConfig.env("PASS"),
                                            "secret"),
                                    new JsonMapper());

            search.count("notes", null);

            assertEquals(
                    "Basic ZWxhc3RpYzpzZWNyZXQ=", stub.requests().get(0).header("Authorization"));
        }
    }
}
