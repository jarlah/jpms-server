package dev.jarl.jpmsserver.postgres;

import java.sql.SQLException;

public final class UncheckedSqlException extends RuntimeException {

    public UncheckedSqlException(SQLException cause) {
        super(cause);
    }
}
