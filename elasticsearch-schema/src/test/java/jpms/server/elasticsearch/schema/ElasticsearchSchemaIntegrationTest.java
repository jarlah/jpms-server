package jpms.server.elasticsearch.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import jpms.server.core.config.Config;
import jpms.server.core.notes.NoteService;
import jpms.server.elasticsearch.EsRestClientProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
class ElasticsearchSchemaIntegrationTest {

    private static final ObjectMapper JSON = new JsonMapper();

    @Container
    @SuppressWarnings("resource") // Closed by the Testcontainers JUnit extension.
    private static final ElasticsearchContainer ELASTICSEARCH =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.4.2")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @BeforeAll
    static void applyProductionSchema() {
        assertEquals(2, Schema.apply(config(), JSON));
        assertEquals(2, Schema.apply(config(), JSON), "schema application must be idempotent");
    }

    @Test
    void productionSchemaSupportsBothIndexesAndRealSearch() {
        var search = new EsRestClientProvider().create(config(), JSON);

        assertTrue(search.indexExists(NoteService.INDEX));
        assertTrue(search.indexExists(ElasticsearchSchemaProvider.NOTE_SUGGESTIONS_INDEX));

        var note = JSON.createObjectNode();
        note.put("title", "Testcontainers integration");
        note.put("body", "Real Elasticsearch verifies the production mapping");
        note.put("created_at", "2026-07-19T12:00:00Z");
        search.indexDoc(NoteService.INDEX, "1", note);
        search.refresh(NoteService.INDEX);

        var request = JSON.createObjectNode();
        request.putObject("query").putObject("match").put("title", "testcontainers");
        var result = search.search(NoteService.INDEX, request);

        assertEquals(1, result.total());
        assertEquals("1", result.hits().getFirst().id());

        var suggestion = JSON.createObjectNode();
        suggestion.put("note_id", "1");
        suggestion.put("suggest", "Testcontainers integration");
        suggestion.put("updated_at", "2026-07-19T12:00:00Z");
        search.indexDoc(ElasticsearchSchemaProvider.NOTE_SUGGESTIONS_INDEX, "1", suggestion);
        search.refresh(ElasticsearchSchemaProvider.NOTE_SUGGESTIONS_INDEX);

        assertEquals(1, search.count(ElasticsearchSchemaProvider.NOTE_SUGGESTIONS_INDEX, null));
    }

    private static Config config() {
        return Config.of(
                Map.of(
                        "jpms.server.elasticsearch.target",
                        "http://" + ELASTICSEARCH.getHttpHostAddress()));
    }
}
