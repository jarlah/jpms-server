package dev.jarl.jpmsserver.core.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import dev.jarl.jpmsserver.core.notes.InMemoryNoteRepository;
import dev.jarl.jpmsserver.core.notes.NoteService;
import dev.jarl.jpmsserver.core.search.InMemorySearchIndex;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** End-to-end for core HTTP and JSON, with in-memory infrastructure swapped in. */
class HttpApiTest {

    private static final ObjectMapper json = new JsonMapper();

    private static HttpServer server;
    private static URI base;

    @BeforeEach
    void startServer() throws Exception {
        if (server != null) {
            server.stop(0);
        }
        NoteService notes =
                new NoteService(new InMemoryNoteRepository(), new InMemorySearchIndex(), json);
        notes.ensureSearchIndex();
        server = HttpApi.start(0, notes, json);
        base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fullNoteLifecycle() throws Exception {
        assertEquals(200, get("/healthz").status());

        Response created =
                post(
                        "/notes",
                        """
                {"title":"Groceries","body":"buy milk and eggs"}""");
        assertEquals(201, created.status());
        long id = json.readTree(created.body()).path("id").asLong();

        Response fetched = get("/notes/" + id);
        assertEquals(200, fetched.status());
        assertEquals("Groceries", json.readTree(fetched.body()).path("title").asString());

        Response found = get("/notes/_search?q=milk");
        assertEquals(200, found.status());
        JsonNode searchBody = json.readTree(found.body());
        assertEquals(1, searchBody.path("total").asLong());
        assertEquals(
                "Groceries",
                searchBody.path("hits").path(0).path("source").path("title").asString());

        assertEquals(204, delete("/notes/" + id).status());
        assertEquals(404, get("/notes/" + id).status());
        assertEquals(0, json.readTree(get("/notes/_search?q=milk").body()).path("total").asLong());
    }

    @Test
    void createWithoutTitleIsRejected() throws Exception {
        assertEquals(
                400,
                post(
                                "/notes",
                                """
                {"body":"no title"}""")
                        .status());
    }

    @Test
    void unknownRouteIs404() throws Exception {
        assertEquals(404, get("/nope").status());
    }

    private static Response get(String path) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) base.resolve(path).toURL().openConnection();
        connection.setRequestMethod("GET");
        return response(connection);
    }

    private static Response post(String path, String body) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) base.resolve(path).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.getOutputStream().write(body.getBytes());
        return response(connection);
    }

    private static Response delete(String path) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) base.resolve(path).toURL().openConnection();
        connection.setRequestMethod("DELETE");
        return response(connection);
    }

    private static Response response(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        var stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = stream == null ? "" : new String(stream.readAllBytes());
        return new Response(status, body);
    }

    private record Response(int status, String body) {}
}
