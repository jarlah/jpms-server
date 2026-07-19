package dev.jarl.jpmsserver.elasticsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jarl.jpmsserver.core.search.BulkOp;
import dev.jarl.jpmsserver.core.search.BulkResult;
import dev.jarl.jpmsserver.core.search.SearchResult;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class EsRestClientTest {

    private final ObjectMapper json = new JsonMapper();
    private StubEsServer stub;
    private EsRestClient client;

    @BeforeEach
    void setUp() throws IOException {
        stub = new StubEsServer();
        client =
                new EsRestClient(
                        stub.uri(),
                        null,
                        json,
                        HttpClient.newHttpClient(),
                        3,
                        Duration.ofMillis(1));
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @Test
    void bulkBuildsNdjsonAndSurfacesPerItemFailures() throws Exception {
        stub.enqueue(
                StubEsServer.Response.of(
                        200,
                        """
                {"took":5,"errors":true,"items":[
                  {"index":{"_index":"notes","_id":"1","status":201,"result":"created"}},
                  {"index":{"_index":"notes","_id":"2","status":400,
                            "error":{"type":"mapper_parsing_exception","reason":"failed to parse field [created_at]"}}}
                ]}"""));

        ObjectNode doc = json.createObjectNode().put("title", "hello");
        BulkResult result =
                client.bulk(
                        List.of(
                                new BulkOp.Index("notes", "1", doc),
                                new BulkOp.Delete("notes", "2")));

        assertTrue(result.errors());
        assertEquals(1, result.failures().size());
        BulkResult.Failure failure = result.failures().get(0);
        assertEquals("index", failure.op());
        assertEquals("2", failure.id());
        assertEquals(400, failure.status());
        assertTrue(failure.reason().contains("failed to parse"));

        StubEsServer.Request request = stub.requests().get(0);
        assertEquals("POST", request.method());
        assertEquals("/_bulk", request.path());
        assertEquals("application/x-ndjson", request.header("Content-Type"));
        assertTrue(request.body().endsWith("\n"), "NDJSON must be newline-terminated");
        String[] lines = request.body().split("\n");
        assertEquals(3, lines.length, "index action + source, delete action");
        assertEquals("notes", json.readTree(lines[0]).path("index").path("_index").asString());
        assertEquals("1", json.readTree(lines[0]).path("index").path("_id").asString());
        assertEquals("hello", json.readTree(lines[1]).path("title").asString());
        assertEquals("2", json.readTree(lines[2]).path("delete").path("_id").asString());
    }

    @Test
    void bulkDeleteNotFoundIsNotAFailure() {
        stub.enqueue(
                StubEsServer.Response.of(
                        200,
                        """
                {"took":1,"errors":false,"items":[
                  {"delete":{"_index":"notes","_id":"9","status":404,"result":"not_found"}}
                ]}"""));

        BulkResult result = client.bulk(List.of(new BulkOp.Delete("notes", "9")));

        assertFalse(result.errors());
        assertTrue(result.failures().isEmpty());
    }

    @Test
    void retriesOn503ThenSucceeds() {
        stub.enqueue(
                StubEsServer.Response.of(
                        503,
                        """
                        {"error":"unavailable"}"""),
                StubEsServer.Response.of(
                        200,
                        """
                        {"_id":"1","found":true,"_source":{"title":"hello"}}"""));

        Optional<ObjectNode> doc = client.getDoc("notes", "1");

        assertEquals("hello", doc.orElseThrow().path("title").asString());
        assertEquals(2, stub.requests().size());
    }

    @Test
    void givesUpAfterMaxAttemptsOn429() {
        stub.enqueue(
                StubEsServer.Response.of(429, "{}"),
                StubEsServer.Response.of(429, "{}"),
                StubEsServer.Response.of(429, "{}"));

        EsException e = assertThrows(EsException.class, () -> client.getDoc("notes", "1"));

        assertEquals(429, e.status());
        assertEquals(3, stub.requests().size());
    }

    @Test
    void missingDocIsEmpty() {
        stub.enqueue(
                StubEsServer.Response.of(
                        404,
                        """
                {"_index":"notes","_id":"9","found":false}"""));

        assertTrue(client.getDoc("notes", "9").isEmpty());
    }

    @Test
    void parsesSearchHits() {
        stub.enqueue(
                StubEsServer.Response.of(
                        200,
                        """
                {"hits":{"total":{"value":42,"relation":"eq"},"hits":[
                  {"_id":"1","_score":1.5,"_source":{"title":"first"}},
                  {"_id":"2","_score":0.5,"_source":{"title":"second"}}
                ]}}"""));

        ObjectNode request = json.createObjectNode();
        request.putObject("query").putObject("match_all");
        SearchResult result = client.search("notes", request);

        assertEquals(42, result.total());
        assertEquals(2, result.hits().size());
        assertEquals("1", result.hits().get(0).id());
        assertEquals(1.5, result.hits().get(0).score());
        assertEquals("first", result.hits().get(0).source().path("title").asString());
    }

    @Test
    void sendsAuthorizationHeader() {
        var authed =
                new EsRestClient(
                        stub.uri(),
                        "ApiKey secret123",
                        json,
                        HttpClient.newHttpClient(),
                        3,
                        Duration.ofMillis(1));
        stub.enqueue(
                StubEsServer.Response.of(
                        200,
                        """
                {"count":7}"""));

        assertEquals(7, authed.count("notes", null));
        assertEquals("ApiKey secret123", stub.requests().get(0).header("Authorization"));
    }
}
