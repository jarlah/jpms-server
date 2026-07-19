package jpms.server.core.notes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jpms.server.core.db.NoteRepository;

public final class InMemoryNoteRepository implements NoteRepository {

    private final Map<Long, Note> notes = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized Note insert(String title, String body) {
        Note note =
                new Note(
                        nextId++,
                        title,
                        body,
                        OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS));
        notes.put(note.id(), note);
        return note;
    }

    @Override
    public synchronized Optional<Note> findById(long id) {
        return Optional.ofNullable(notes.get(id));
    }

    @Override
    public synchronized List<Note> findAll() {
        return new ArrayList<>(notes.values());
    }

    @Override
    public synchronized boolean delete(long id) {
        return notes.remove(id) != null;
    }
}
