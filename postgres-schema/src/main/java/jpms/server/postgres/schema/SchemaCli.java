package jpms.server.postgres.schema;

public final class SchemaCli {

    static void main(String[] args) {
        int statements = Schema.apply(System.getenv());
        System.out.println("applied " + statements + " PostgreSQL schema statement(s)");
    }

    private SchemaCli() {}
}
