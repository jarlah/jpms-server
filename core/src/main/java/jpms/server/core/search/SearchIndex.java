package jpms.server.core.search;

import java.util.List;
import java.util.Optional;
import tools.jackson.databind.node.ObjectNode;

/** The thin search/index surface this app needs, independent of the backing engine. */
public interface SearchIndex {

    void indexDoc(String index, String id, ObjectNode doc);

    Optional<ObjectNode> getDoc(String index, String id);

    /**
     * @return true if the document existed
     */
    boolean deleteDoc(String index, String id);

    /**
     * @param request a full search request body (query, size, sort, ...)
     */
    SearchResult search(String index, ObjectNode request);

    /**
     * @param query a query node, or null to count everything
     */
    long count(String index, ObjectNode query);

    BulkResult bulk(List<BulkOp> ops);

    boolean indexExists(String index);

    /**
     * @param definition index body (settings/mappings), or null for defaults
     */
    void createIndex(String index, ObjectNode definition);

    void deleteIndex(String index);

    /** Makes indexed documents visible to search immediately. */
    void refresh(String index);
}
