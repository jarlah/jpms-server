package dev.jarl.jpmsserver.core.db;

import dev.jarl.jpmsserver.core.notes.Note;
import java.util.List;
import java.util.Optional;

public interface NoteRepository {

    Note insert(String title, String body);

    Optional<Note> findById(long id);

    List<Note> findAll();

    boolean delete(long id);
}
