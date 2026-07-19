package dev.jarl.jpmsserver.elasticsearch;

import dev.jarl.jpmsserver.core.search.BulkOp;
import dev.jarl.jpmsserver.core.search.BulkResult;
import dev.jarl.jpmsserver.core.search.SearchIndex;
import dev.jarl.jpmsserver.core.search.SearchResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Thin Elasticsearch REST client over {@link HttpClient}: doc CRUD, _search, _count, _bulk (NDJSON,
 * per-item error checking) and index management, with retry/backoff on 429/5xx and transport
 * errors. Retrying is safe because every operation uses explicit document ids (idempotent). Assumes
 * a single URL (managed cluster or load balancer); node sniffing and dead-node marking are
 * deliberately out of scope.
 */
public final class EsRestClient implements SearchIndex {

    private static final String JSON = "application/json";
    private static final String NDJSON = "application/x-ndjson";
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 502, 503, 504);

    private final URI base;
    private final String authHeader; // null = no Authorization header
    private final ObjectMapper json;
    private final HttpClient http;
    private final int maxAttempts;
    private final Duration retryBase;

    public EsRestClient(URI base, String authHeader, ObjectMapper json) {
        this(
                base,
                authHeader,
                json,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                4,
                Duration.ofMillis(250));
    }

    public EsRestClient(
            URI base,
            String authHeader,
            ObjectMapper json,
            HttpClient http,
            int maxAttempts,
            Duration retryBase) {
        String uri = base.toString();
        this.base = URI.create(uri.endsWith("/") ? uri : uri + "/");
        this.authHeader = authHeader;
        this.json = json;
        this.http = http;
        this.maxAttempts = maxAttempts;
        this.retryBase = retryBase;
    }

    @Override
    public void indexDoc(String index, String id, ObjectNode doc) {
        expect(exchange("PUT", enc(index) + "/_doc/" + enc(id), JSON, bytes(doc)), 200, 201);
    }

    @Override
    public Optional<ObjectNode> getDoc(String index, String id) {
        HttpResponse<byte[]> response =
                exchange("GET", enc(index) + "/_doc/" + enc(id), null, null);
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        expect(response, 200);
        return Optional.of(objectAt(read(response), "_source"));
    }

    @Override
    public boolean deleteDoc(String index, String id) {
        HttpResponse<byte[]> response =
                exchange("DELETE", enc(index) + "/_doc/" + enc(id), null, null);
        if (response.statusCode() == 404) {
            return false;
        }
        expect(response, 200);
        return true;
    }

    @Override
    public SearchResult search(String index, ObjectNode request) {
        HttpResponse<byte[]> response =
                exchange("POST", enc(index) + "/_search", JSON, bytes(request));
        expect(response, 200);
        JsonNode hits = read(response).path("hits");
        List<SearchResult.Hit> parsed = new ArrayList<>();
        for (JsonNode hit : hits.path("hits")) {
            parsed.add(
                    new SearchResult.Hit(
                            hit.path("_id").asString(),
                            hit.path("_score").asDouble(0),
                            objectAt(hit, "_source")));
        }
        return new SearchResult(hits.path("total").path("value").asLong(), List.copyOf(parsed));
    }

    @Override
    public long count(String index, ObjectNode query) {
        byte[] body = query == null ? null : bytes(json.createObjectNode().set("query", query));
        HttpResponse<byte[]> response = exchange("POST", enc(index) + "/_count", JSON, body);
        expect(response, 200);
        return read(response).path("count").asLong();
    }

    @Override
    public BulkResult bulk(List<BulkOp> ops) {
        if (ops.isEmpty()) {
            return new BulkResult(false, List.of());
        }
        HttpResponse<byte[]> response = exchange("POST", "_bulk", NDJSON, ndjson(ops));
        expect(response, 200);
        JsonNode root = read(response);
        // HTTP 200 does not mean success: every item must be checked for an error object.
        List<BulkResult.Failure> failures = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            var properties = item.properties();
            if (properties.isEmpty()) {
                continue;
            }
            Map.Entry<String, JsonNode> entry = properties.iterator().next();
            JsonNode result = entry.getValue();
            if (result.has("error")) {
                failures.add(
                        new BulkResult.Failure(
                                entry.getKey(),
                                result.path("_index").asString(),
                                result.path("_id").asString(),
                                result.path("status").asInt(),
                                result.path("error")
                                        .path("reason")
                                        .asString(result.path("error").toString())));
            }
        }
        return new BulkResult(root.path("errors").asBoolean(), List.copyOf(failures));
    }

    @Override
    public boolean indexExists(String index) {
        HttpResponse<byte[]> response = exchange("HEAD", enc(index), null, null);
        if (response.statusCode() == 404) {
            return false;
        }
        expect(response, 200);
        return true;
    }

    @Override
    public void createIndex(String index, ObjectNode definition) {
        expect(
                exchange("PUT", enc(index), JSON, definition == null ? null : bytes(definition)),
                200);
    }

    @Override
    public void deleteIndex(String index) {
        expect(exchange("DELETE", enc(index), null, null), 200);
    }

    @Override
    public void refresh(String index) {
        expect(exchange("POST", enc(index) + "/_refresh", null, null), 200);
    }

    private byte[] ndjson(List<BulkOp> ops) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (BulkOp op : ops) {
            switch (op) {
                case BulkOp.Index(String index, String id, ObjectNode doc) -> {
                    writeLine(out, action("index", index, id));
                    writeLine(out, doc);
                }
                case BulkOp.Delete(String index, String id) ->
                        writeLine(out, action("delete", index, id));
            }
        }
        return out.toByteArray();
    }

    private ObjectNode action(String op, String index, String id) {
        ObjectNode line = json.createObjectNode();
        line.putObject(op).put("_index", index).put("_id", id);
        return line;
    }

    private void writeLine(ByteArrayOutputStream out, JsonNode node) {
        out.writeBytes(json.writeValueAsBytes(node));
        out.write('\n');
    }

    private HttpResponse<byte[]> exchange(
            String method, String path, String contentType, byte[] body) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(base.resolve(path))
                        .timeout(Duration.ofSeconds(30))
                        .method(
                                method,
                                body == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofByteArray(body));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        if (authHeader != null) {
            builder.header("Authorization", authHeader);
        }
        HttpRequest request = builder.build();

        EsException last = null;
        for (int attempt = 1; ; attempt++) {
            Long retryAfterMillis = null;
            try {
                HttpResponse<byte[]> response =
                        http.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (!RETRYABLE_STATUSES.contains(response.statusCode())) {
                    return response;
                }
                last = new EsException(response.statusCode(), bodyText(response));
                retryAfterMillis = retryAfterMillis(response);
            } catch (IOException e) {
                last =
                        new EsException(
                                method + " " + request.uri() + " failed: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EsException(method + " " + request.uri() + " interrupted", e);
            }
            if (attempt >= maxAttempts) {
                throw last;
            }
            sleep(retryAfterMillis != null ? retryAfterMillis : backoffMillis(attempt));
        }
    }

    private long backoffMillis(int attempt) {
        long base = retryBase.toMillis() << (attempt - 1);
        return base + ThreadLocalRandom.current().nextLong(base / 2 + 1);
    }

    private static Long retryAfterMillis(HttpResponse<?> response) {
        return response.headers()
                .firstValue("Retry-After")
                .map(
                        value -> {
                            try {
                                return Long.parseLong(value.trim()) * 1000;
                            } catch (NumberFormatException e) {
                                return null;
                            }
                        })
                .orElse(null);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EsException("interrupted during retry backoff", e);
        }
    }

    private static void expect(HttpResponse<byte[]> response, int... allowed) {
        for (int status : allowed) {
            if (response.statusCode() == status) {
                return;
            }
        }
        throw new EsException(response.statusCode(), bodyText(response));
    }

    private static String bodyText(HttpResponse<byte[]> response) {
        String text = new String(response.body(), StandardCharsets.UTF_8);
        return text.length() > 500 ? text.substring(0, 500) + "…" : text;
    }

    private JsonNode read(HttpResponse<byte[]> response) {
        return json.readTree(response.body());
    }

    private byte[] bytes(JsonNode node) {
        return json.writeValueAsBytes(node);
    }

    private ObjectNode objectAt(JsonNode node, String field) {
        return node.get(field) instanceof ObjectNode object ? object : json.createObjectNode();
    }

    private static String enc(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
