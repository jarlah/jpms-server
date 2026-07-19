package jpms.server.elasticsearch;

public final class EsException extends RuntimeException {

    private final int status; // 0 when the failure happened before an HTTP response arrived

    public EsException(int status, String message) {
        super("Elasticsearch returned HTTP " + status + ": " + message);
        this.status = status;
    }

    public EsException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    public int status() {
        return status;
    }
}
