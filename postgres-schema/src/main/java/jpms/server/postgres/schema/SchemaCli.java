package jpms.server.postgres.schema;

import java.util.Map;
import jpms.server.core.config.Config;

public final class SchemaCli {

    static void main(String[] args) {
        Config config = Config.of(Map.of()).withEnvOverrides(System.getenv());
        int statements = Schema.apply(config);
        System.out.println("applied " + statements + " PostgreSQL schema statement(s)");
    }

    private SchemaCli() {}
}
