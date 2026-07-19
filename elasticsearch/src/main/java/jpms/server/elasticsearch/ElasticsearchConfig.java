package jpms.server.elasticsearch;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jpms.server.core.config.Config;

record ElasticsearchConfig(URI base, String authHeader) {

    private static final String PREFIX = "jpms.server.elasticsearch.";

    static ElasticsearchConfig from(Config config) {
        return new ElasticsearchConfig(
                URI.create(config.getString(path("target"), "http://localhost:9200")),
                authHeader(config));
    }

    static String path(String name) {
        return PREFIX + name;
    }

    private static String authHeader(Config config) {
        String apiKey = config.getString(path("api.key"), null);
        if (apiKey != null) {
            return "ApiKey " + apiKey;
        }
        String user = config.getString(path("user"), null);
        if (user != null) {
            String credentials = user + ":" + config.getString(path("pass"), "");
            return "Basic "
                    + Base64.getEncoder()
                            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }
}
