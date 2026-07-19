package jpms.server.core.notes;

import java.util.List;
import java.util.Optional;
import jpms.server.core.db.NoteRepository;
import jpms.server.core.search.BulkOp;
import jpms.server.core.search.BulkResult;
import jpms.server.core.search.SearchIndex;
import jpms.server.core.search.SearchResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class NoteService {

    public static final String INDEX = "notes";

    private final NoteRepository repository;
    private final SearchIndex searchIndex;
    private final ObjectMapper json;

    public NoteService(NoteRepository repository, SearchIndex searchIndex, ObjectMapper json) {
        this.repository = repository;
        this.searchIndex = searchIndex;
        this.json = json;
    }

    public void ensureSearchIndex() {
        if (!searchIndex.indexExists(INDEX)) {
            ObjectNode definition = json.createObjectNode();
            ObjectNode properties = definition.putObject("mappings").putObject("properties");
            properties.putObject("title").put("type", "text");
            properties.putObject("body").put("type", "text");
            properties.putObject("created_at").put("type", "date");
            searchIndex.createIndex(INDEX, definition);
        }
    }

    public Note create(String title, String body) {
        Note note = repository.insert(title, body);
        searchIndex.indexDoc(INDEX, Long.toString(note.id()), doc(note));
        return note;
    }

    public Optional<Note> get(long id) {
        return repository.findById(id);
    }

    public List<Note> list() {
        return repository.findAll();
    }

    public boolean delete(long id) {
        boolean existed = repository.delete(id);
        if (existed) {
            searchIndex.deleteDoc(INDEX, Long.toString(id));
        }
        return existed;
    }

    public SearchResult search(String query, int size) {
        ObjectNode request = json.createObjectNode();
        request.put("size", size);
        ObjectNode multiMatch = request.putObject("query").putObject("multi_match");
        multiMatch.put("query", query);
        multiMatch.putArray("fields").add("title^2").add("body");
        return searchIndex.search(INDEX, request);
    }

    /** Rebuilds the search index from the database via _bulk. Caller must inspect the result. */
    public BulkResult reindexAll() {
        List<BulkOp> ops =
                repository.findAll().stream()
                        .map(
                                note ->
                                        (BulkOp)
                                                new BulkOp.Index(
                                                        INDEX, Long.toString(note.id()), doc(note)))
                        .toList();
        return searchIndex.bulk(ops);
    }

    private ObjectNode doc(Note note) {
        ObjectNode doc = json.createObjectNode();
        doc.put("title", note.title());
        doc.put("body", note.body());
        doc.put("created_at", note.createdAt().toString());
        return doc;
    }
}
