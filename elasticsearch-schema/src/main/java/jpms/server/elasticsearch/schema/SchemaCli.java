package jpms.server.elasticsearch.schema;

import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class SchemaCli {

    static void main(String[] args) {
        Map<String, String> env = System.getenv();
        ObjectMapper json = new JsonMapper();

        int indexes = Schema.apply(env, json);
        System.out.println(
                "upserted "
                        + indexes
                        + " Elasticsearch index"
                        + (indexes == 1 ? "" : "es")
                        + " at "
                        + env.getOrDefault(
                                "JPMS_SERVER_ELASTICSEARCH_TARGET", "http://localhost:9200"));
    }

    private SchemaCli() {}
}
