package dev.jarl.jpmsserver.elasticsearch;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/** Scripted fake Elasticsearch endpoint: dequeues canned responses, records every request. */
final class StubEsServer implements AutoCloseable {

    record Request(String method, String path, Map<String, List<String>> headers, String body) {

        String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue().get(0);
                }
            }
            return null;
        }
    }

    record Response(int status, String body) {

        static Response of(int status, String body) {
            return new Response(status, body);
        }
    }

    private final HttpServer server;
    private final Deque<Response> responses = new ArrayDeque<>();
    private final List<Request> requests = new ArrayList<>();

    StubEsServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
                "/",
                exchange -> {
                    String body =
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8);
                    Response response;
                    synchronized (this) {
                        requests.add(
                                new Request(
                                        exchange.getRequestMethod(),
                                        exchange.getRequestURI().getPath(),
                                        Map.copyOf(exchange.getRequestHeaders()),
                                        body));
                        response = responses.isEmpty() ? Response.of(200, "{}") : responses.poll();
                    }
                    byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    if ("HEAD".equals(exchange.getRequestMethod()) || bytes.length == 0) {
                        exchange.sendResponseHeaders(response.status(), -1);
                    } else {
                        exchange.sendResponseHeaders(response.status(), bytes.length);
                        exchange.getResponseBody().write(bytes);
                    }
                    exchange.close();
                });
        server.start();
    }

    synchronized void enqueue(Response... scripted) {
        responses.addAll(List.of(scripted));
    }

    synchronized List<Request> requests() {
        return List.copyOf(requests);
    }

    URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
