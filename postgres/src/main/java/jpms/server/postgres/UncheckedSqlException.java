package jpms.server.postgres;

import java.sql.SQLException;

public final class UncheckedSqlException extends RuntimeException {

    public UncheckedSqlException(SQLException cause) {
        super(cause);
    }
}
