package dev.jarl.jpmsserver.elasticsearch;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

record ElasticsearchConfig(URI base, String authHeader) {

    private static final String ENV_PREFIX = "JPMS_SERVER_ELASTICSEARCH_";

    static ElasticsearchConfig from(Map<String, String> values) {
        return new ElasticsearchConfig(
                URI.create(values.getOrDefault(env("TARGET"), "http://localhost:9200")),
                authHeader(values));
    }

    static String env(String name) {
        return ENV_PREFIX + name;
    }

    private static String authHeader(Map<String, String> values) {
        String apiKey = values.get(env("API_KEY"));
        if (apiKey != null) {
            return "ApiKey " + apiKey;
        }
        String user = values.get(env("USER"));
        if (user != null) {
            String credentials = user + ":" + values.getOrDefault(env("PASS"), "");
            return "Basic "
                    + Base64.getEncoder()
                            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }
}
