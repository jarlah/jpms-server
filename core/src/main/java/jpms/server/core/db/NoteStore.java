package jpms.server.core.db;

public interface NoteStore extends AutoCloseable {

    NoteRepository repository();

    @Override
    void close();
}
