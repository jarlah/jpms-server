package jpms.server.core.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jpms.server.core.db.NoteRepository;
import jpms.server.core.search.InMemorySearchIndex;
import jpms.server.core.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class NoteServiceTest {

    private NoteRepository repository;
    private NoteService service;

    @BeforeEach
    void resetState() {
        repository = new InMemoryNoteRepository();
        service = new NoteService(repository, new InMemorySearchIndex(), new JsonMapper());
        service.ensureSearchIndex();
    }

    @Test
    void createStoresRowAndIndexesDoc() {
        Note note = service.create("Groceries", "buy milk and eggs");

        assertTrue(service.get(note.id()).isPresent());
        SearchResult result = service.search("milk", 10);
        assertEquals(1, result.total());
        assertEquals("Groceries", result.hits().get(0).source().path("title").asString());
    }

    @Test
    void searchMatchesTitleToo() {
        service.create("Groceries", "buy milk and eggs");

        assertEquals(1, service.search("groceries", 10).total());
        assertEquals(0, service.search("unrelated", 10).total());
    }

    @Test
    void deleteRemovesRowAndDoc() {
        Note note = service.create("Groceries", "buy milk and eggs");

        assertTrue(service.delete(note.id()));

        assertTrue(service.get(note.id()).isEmpty());
        assertEquals(0, service.search("milk", 10).total());
        assertFalse(service.delete(note.id()));
    }

    @Test
    void reindexAllBulkIndexesEveryRow() {
        repository.insert("One", "first note about apples");
        repository.insert("Two", "second note about oranges");
        assertEquals(0, service.search("apples", 10).total());

        var result = service.reindexAll();

        assertFalse(result.errors());
        assertEquals(1, service.search("apples", 10).total());
        assertEquals(2, service.search("note", 10).total());
    }
}
