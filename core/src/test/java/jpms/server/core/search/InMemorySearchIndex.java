package jpms.server.core.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test double for {@link SearchIndex}, swapped in the way H2 swaps for Postgres. Supports the query
 * subset this app uses: match_all, term, match (OR semantics like real ES), multi_match (boosts
 * stripped), bool (must/filter/must_not). Anything else throws so a test never silently passes on
 * an unsupported query.
 */
public final class InMemorySearchIndex implements SearchIndex {

    private final Map<String, Map<String, ObjectNode>> indices = new LinkedHashMap<>();

    @Override
    public synchronized void indexDoc(String index, String id, ObjectNode doc) {
        docs(index).put(id, doc.deepCopy());
    }

    @Override
    public synchronized Optional<ObjectNode> getDoc(String index, String id) {
        return Optional.ofNullable(docs(index).get(id)).map(ObjectNode::deepCopy);
    }

    @Override
    public synchronized boolean deleteDoc(String index, String id) {
        return docs(index).remove(id) != null;
    }

    @Override
    public synchronized SearchResult search(String index, ObjectNode request) {
        JsonNode query = request.path("query");
        int size = request.path("size").asInt(10);
        List<SearchResult.Hit> hits = new ArrayList<>();
        for (Map.Entry<String, ObjectNode> entry : docs(index).entrySet()) {
            if (matches(query, entry.getValue())) {
                hits.add(new SearchResult.Hit(entry.getKey(), 1.0, entry.getValue().deepCopy()));
            }
        }
        return new SearchResult(
                hits.size(), List.copyOf(hits.subList(0, Math.min(size, hits.size()))));
    }

    @Override
    public synchronized long count(String index, ObjectNode query) {
        JsonNode effective =
                query == null ? tools.jackson.databind.node.MissingNode.getInstance() : query;
        return docs(index).values().stream().filter(doc -> matches(effective, doc)).count();
    }

    @Override
    public synchronized BulkResult bulk(List<BulkOp> ops) {
        for (BulkOp op : ops) {
            switch (op) {
                case BulkOp.Index(String index, String id, ObjectNode doc) ->
                        indexDoc(index, id, doc);
                case BulkOp.Delete(String index, String id) -> deleteDoc(index, id);
            }
        }
        // Like real ES: deleting a missing doc is a 404 item without an error object, so errors
        // stays false.
        return new BulkResult(false, List.of());
    }

    @Override
    public synchronized boolean indexExists(String index) {
        return indices.containsKey(index);
    }

    @Override
    public synchronized void createIndex(String index, ObjectNode definition) {
        indices.putIfAbsent(index, new LinkedHashMap<>());
    }

    @Override
    public synchronized void deleteIndex(String index) {
        indices.remove(index);
    }

    @Override
    public void refresh(String index) {
        // in-memory writes are immediately visible
    }

    private Map<String, ObjectNode> docs(String index) {
        return indices.computeIfAbsent(index, k -> new LinkedHashMap<>());
    }

    private boolean matches(JsonNode query, ObjectNode doc) {
        if (query.isMissingNode() || query.isNull() || query.has("match_all")) {
            return true;
        }
        if (query.has("term")) {
            Map.Entry<String, JsonNode> term = query.get("term").properties().iterator().next();
            JsonNode value =
                    term.getValue().isObject() ? term.getValue().path("value") : term.getValue();
            return doc.path(term.getKey()).asString().equals(value.asString());
        }
        if (query.has("match")) {
            Map.Entry<String, JsonNode> match = query.get("match").properties().iterator().next();
            JsonNode value =
                    match.getValue().isObject() ? match.getValue().path("query") : match.getValue();
            return anyTokenMatches(doc.path(match.getKey()).asString(), value.asString());
        }
        if (query.has("multi_match")) {
            JsonNode multiMatch = query.get("multi_match");
            String text = multiMatch.path("query").asString();
            for (JsonNode field : multiMatch.path("fields")) {
                String name = field.asString().split("\\^")[0];
                if (anyTokenMatches(doc.path(name).asString(), text)) {
                    return true;
                }
            }
            return false;
        }
        if (query.has("bool")) {
            JsonNode bool = query.get("bool");
            for (JsonNode clause : bool.path("must")) {
                if (!matches(clause, doc)) {
                    return false;
                }
            }
            for (JsonNode clause : bool.path("filter")) {
                if (!matches(clause, doc)) {
                    return false;
                }
            }
            for (JsonNode clause : bool.path("must_not")) {
                if (matches(clause, doc)) {
                    return false;
                }
            }
            return true;
        }
        throw new UnsupportedOperationException(
                "query not supported by InMemorySearchIndex: " + query);
    }

    private static boolean anyTokenMatches(String fieldText, String queryText) {
        Set<String> fieldTokens = tokenize(fieldText);
        for (String token : tokenize(queryText)) {
            if (fieldTokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
