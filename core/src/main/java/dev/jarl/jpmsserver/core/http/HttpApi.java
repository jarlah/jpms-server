package dev.jarl.jpmsserver.core.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.jarl.jpmsserver.core.notes.Note;
import dev.jarl.jpmsserver.core.notes.NoteService;
import dev.jarl.jpmsserver.core.search.BulkResult;
import dev.jarl.jpmsserver.core.search.SearchResult;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class HttpApi {

    private final NoteService notes;
    private final ObjectMapper json;

    private HttpApi(NoteService notes, ObjectMapper json) {
        this.notes = notes;
        this.json = json;
    }

    /**
     * @param port 0 picks an ephemeral port (used by tests)
     */
    public static HttpServer start(int port, NoteService notes, ObjectMapper json)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", new HttpApi(notes, json)::handle);
        server.start();
        return server;
    }

    private void handle(HttpExchange exchange) {
        try {
            route(exchange);
        } catch (NumberFormatException e) {
            reply(exchange, 400, error("invalid number"));
        } catch (Exception e) {
            reply(exchange, 500, error(e.toString()));
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equals(method) && "/healthz".equals(path)) {
            reply(exchange, 200, json.createObjectNode().put("ok", true));
        } else if ("/notes".equals(path)) {
            switch (method) {
                case "POST" -> createNote(exchange);
                case "GET" -> listNotes(exchange);
                default -> reply(exchange, 405, error("method not allowed"));
            }
        } else if ("/notes/_search".equals(path) && "GET".equals(method)) {
            search(exchange);
        } else if ("/notes/_reindex".equals(path) && "POST".equals(method)) {
            reindex(exchange);
        } else if (path.startsWith("/notes/")) {
            long id = Long.parseLong(path.substring("/notes/".length()));
            switch (method) {
                case "GET" -> getNote(exchange, id);
                case "DELETE" -> deleteNote(exchange, id);
                default -> reply(exchange, 405, error("method not allowed"));
            }
        } else {
            reply(exchange, 404, error("no such route"));
        }
    }

    private void createNote(HttpExchange exchange) {
        JsonNode body;
        try {
            body = json.readTree(exchange.getRequestBody());
        } catch (JacksonException e) {
            reply(exchange, 400, error("invalid json"));
            return;
        }
        String title = body.path("title").asString("");
        if (title.isBlank()) {
            reply(exchange, 400, error("title is required"));
            return;
        }
        reply(exchange, 201, noteJson(notes.create(title, body.path("body").asString(""))));
    }

    private void listNotes(HttpExchange exchange) {
        ArrayNode array = json.createArrayNode();
        for (Note note : notes.list()) {
            array.add(noteJson(note));
        }
        reply(exchange, 200, array);
    }

    private void getNote(HttpExchange exchange, long id) {
        Optional<Note> note = notes.get(id);
        if (note.isPresent()) {
            reply(exchange, 200, noteJson(note.get()));
        } else {
            reply(exchange, 404, error("not found"));
        }
    }

    private void deleteNote(HttpExchange exchange, long id) throws IOException {
        if (notes.delete(id)) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            reply(exchange, 404, error("not found"));
        }
    }

    private void search(HttpExchange exchange) {
        Map<String, String> params = queryParams(exchange);
        String q = params.get("q");
        if (q == null || q.isBlank()) {
            reply(exchange, 400, error("q is required"));
            return;
        }
        SearchResult result = notes.search(q, Integer.parseInt(params.getOrDefault("size", "10")));
        ObjectNode out = json.createObjectNode();
        out.put("total", result.total());
        ArrayNode hits = out.putArray("hits");
        for (SearchResult.Hit hit : result.hits()) {
            ObjectNode hitNode = hits.addObject();
            hitNode.put("id", hit.id());
            hitNode.put("score", hit.score());
            hitNode.set("source", hit.source());
        }
        reply(exchange, 200, out);
    }

    private void reindex(HttpExchange exchange) {
        BulkResult result = notes.reindexAll();
        ObjectNode out = json.createObjectNode();
        out.put("errors", result.errors());
        ArrayNode failures = out.putArray("failures");
        for (BulkResult.Failure failure : result.failures()) {
            ObjectNode node = failures.addObject();
            node.put("op", failure.op());
            node.put("index", failure.index());
            node.put("id", failure.id());
            node.put("status", failure.status());
            node.put("reason", failure.reason());
        }
        reply(exchange, 200, out);
    }

    private ObjectNode noteJson(Note note) {
        ObjectNode node = json.createObjectNode();
        node.put("id", note.id());
        node.put("title", note.title());
        node.put("body", note.body());
        node.put("created_at", note.createdAt().toString());
        return node;
    }

    private ObjectNode error(String message) {
        return json.createObjectNode().put("error", message);
    }

    private void reply(HttpExchange exchange, int status, JsonNode body) {
        try {
            byte[] bytes = json.writeValueAsBytes(body);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } catch (IOException e) {
            // client went away; nothing useful to do
        }
    }

    private static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) {
            return params;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            params.put(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }
}
