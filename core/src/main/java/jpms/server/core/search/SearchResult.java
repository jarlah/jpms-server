package jpms.server.core.search;

import java.util.List;
import tools.jackson.databind.node.ObjectNode;

public record SearchResult(long total, List<Hit> hits) {

    public record Hit(String id, double score, ObjectNode source) {}
}
