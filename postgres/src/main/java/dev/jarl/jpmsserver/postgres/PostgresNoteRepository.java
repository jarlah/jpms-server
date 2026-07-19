package dev.jarl.jpmsserver.postgres;

import dev.jarl.jpmsserver.core.db.NoteRepository;
import dev.jarl.jpmsserver.core.notes.Note;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public final class PostgresNoteRepository implements NoteRepository {

    private final DataSource dataSource;

    public PostgresNoteRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Note insert(String title, String body) {
        // Timestamp set here, not by the DB, so the returned Note needs no round trip.
        OffsetDateTime createdAt =
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "INSERT INTO notes (title, body, created_at) VALUES (?, ?, ?)",
                                new String[] {"id"})) {
            statement.setString(1, title);
            statement.setString(2, body);
            statement.setObject(3, createdAt);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new Note(keys.getLong(1), title, body, createdAt);
            }
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    @Override
    public Optional<Note> findById(long id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "SELECT id, title, body, created_at FROM notes WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    @Override
    public List<Note> findAll() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "SELECT id, title, body, created_at FROM notes ORDER BY id");
                ResultSet rs = statement.executeQuery()) {
            List<Note> notes = new ArrayList<>();
            while (rs.next()) {
                notes.add(map(rs));
            }
            return notes;
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("DELETE FROM notes WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    private static Note map(ResultSet rs) throws SQLException {
        return new Note(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getObject("created_at", OffsetDateTime.class));
    }
}
