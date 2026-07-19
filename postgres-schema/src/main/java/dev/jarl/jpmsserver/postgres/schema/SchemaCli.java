package dev.jarl.jpmsserver.postgres.schema;

public final class SchemaCli {

    public static void main(String[] args) {
        int statements = Schema.apply(System.getenv());
        System.out.println("applied " + statements + " PostgreSQL schema statement(s)");
    }

    private SchemaCli() {}
}
