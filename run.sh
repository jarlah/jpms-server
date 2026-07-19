#!/usr/bin/env bash
# Build, then launch fully modular: the app and every dependency on the module path.
set -euo pipefail
cd "$(dirname "$0")"

case "${1:-run}" in
  run)
    ;;
  ddl)
    mvn -q -DskipTests package
    exec java --module-path postgres-schema/target/classes:app/target/modules \
         --add-modules org.postgresql.jdbc \
         -m dev.jarl.jpmsserver.postgres.schema/dev.jarl.jpmsserver.postgres.schema.SchemaCli
    ;;
  *)
    echo "usage: $(basename "$0") [run|ddl]" >&2
    exit 1
    ;;
esac

rm -rf app/target/modules   # copy-dependencies does not purge jars from earlier builds
mvn -q -DskipTests package
exec java --module-path app/target/classes:app/target/modules \
     -m dev.jarl.jpmsserver.app/dev.jarl.jpmsserver.Main
