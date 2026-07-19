package jpms.server.core.db;

import java.util.List;
import java.util.Optional;
import jpms.server.core.notes.Note;

public interface NoteRepository {

    Note insert(String title, String body);

    Optional<Note> findById(long id);

    List<Note> findAll();

    boolean delete(long id);
}
