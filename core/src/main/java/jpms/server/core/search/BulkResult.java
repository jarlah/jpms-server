package jpms.server.core.search;

import java.util.List;

/**
 * Result of a _bulk call. HTTP 200 does not mean success: {@code errors} is the response-level flag
 * and {@code failures} holds every item that carried an error object. A delete of a missing
 * document is a 404 item without an error object — not a failure.
 */
public record BulkResult(boolean errors, List<Failure> failures) {

    public record Failure(String op, String index, String id, int status, String reason) {}
}
