package jpms.server.elasticsearch.schema;

import java.util.Map;
import jpms.server.core.config.Config;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class SchemaCli {

    static void main(String[] args) {
        Config config = Config.of(Map.of()).withEnvOverrides(System.getenv());
        ObjectMapper json = new JsonMapper();

        int indexes = Schema.apply(config, json);
        System.out.println(
                "upserted "
                        + indexes
                        + " Elasticsearch index"
                        + (indexes == 1 ? "" : "es")
                        + " at "
                        + config.getString(
                                "jpms.server.elasticsearch.target", "http://localhost:9200"));
    }

    private SchemaCli() {}
}
