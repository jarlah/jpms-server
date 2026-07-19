package dev.jarl.jpmsserver.core.db;

public interface NoteStore extends AutoCloseable {

    NoteRepository repository();

    @Override
    void close();
}
