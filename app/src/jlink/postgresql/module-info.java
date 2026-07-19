/**
 * Deployment descriptor for pgjdbc, which currently publishes only an Automatic-Module-Name.
 * build-image.sh injects this descriptor into a staged copy of the driver so jlink can consume it.
 */
module org.postgresql.jdbc {
    requires java.desktop;
    requires java.logging;
    requires java.management;
    requires java.naming;
    requires java.security.jgss;
    requires java.sql;
    requires java.transaction.xa;
    requires java.xml;

    provides java.sql.Driver with
            org.postgresql.Driver;
}
