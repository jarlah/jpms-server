package jpms.server.elasticsearch.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import jpms.server.core.notes.NoteService;
import jpms.server.core.search.schema.SearchIndexDefinition;
import jpms.server.core.search.schema.SearchSchemaProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class ElasticsearchSchemaProvider implements SearchSchemaProvider {

    public static final String NOTE_SUGGESTIONS_INDEX = "note_suggestions";

    @Override
    public List<SearchIndexDefinition> indexes(ObjectMapper json) {
        return List.of(
                index(json, NoteService.INDEX, "/elastic/notes.json"),
                index(json, NOTE_SUGGESTIONS_INDEX, "/elastic/note_suggestions.json"));
    }

    private SearchIndexDefinition index(ObjectMapper json, String name, String resource) {
        try (InputStream in = ElasticsearchSchemaProvider.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + resource);
            }
            JsonNode definition = json.readTree(in);
            if (!(definition instanceof ObjectNode object)) {
                throw new IllegalStateException(
                        "resource " + resource + " must contain a JSON object");
            }
            return new SearchIndexDefinition(name, object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
