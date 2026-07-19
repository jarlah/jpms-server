#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ -n "${JAVA_HOME:-}" ]]; then
  jdk="$JAVA_HOME"
else
  java_bin="$(readlink -f "$(command -v java)")"
  jdk="$(dirname "$(dirname "$java_bin")")"
fi

for tool in javac jar jlink; do
  if [[ ! -x "$jdk/bin/$tool" ]]; then
    echo "JDK tool not found: $jdk/bin/$tool" >&2
    exit 1
  fi
done

mvn -q -DskipTests clean package

runtime_modules="app/target/modules"
jlink_modules="app/target/jlink-modules"
descriptor_classes="app/target/jlink-postgresql"
image="app/target/jpms-server"

shopt -s nullglob
postgres_jars=("$runtime_modules"/postgresql-*.jar)
if (( ${#postgres_jars[@]} != 1 )); then
  echo "Expected exactly one PostgreSQL driver in $runtime_modules" >&2
  exit 1
fi
postgres_driver="${postgres_jars[0]}"

mkdir -p "$jlink_modules" "$descriptor_classes"
for module in "$runtime_modules"/*.jar; do
  if [[ "$module" != "$postgres_driver" ]]; then
    cp "$module" "$jlink_modules/"
  fi
done

patched_driver="$jlink_modules/$(basename "$postgres_driver")"
cp "$postgres_driver" "$patched_driver"
"$jdk/bin/javac" \
  --patch-module "org.postgresql.jdbc=$postgres_driver" \
  -d "$descriptor_classes" \
  app/src/jlink/postgresql/module-info.java
"$jdk/bin/jar" --update --file "$patched_driver" \
  -C "$descriptor_classes" module-info.class

"$jdk/bin/jlink" \
  --module-path "$jdk/jmods:app/target/classes:$jlink_modules" \
  --add-modules jpms.server.app \
  --bind-services \
  --launcher jpms-server=jpms.server.app/jpms.server.Main \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress zip-6 \
  --output "$image"

echo "Built $image/bin/jpms-server"
