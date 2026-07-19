# jpms-server

A maximally spartan modular (JPMS) Java service:

- **HTTP**: the JDK's built-in `jdk.httpserver`, one virtual thread per request. No framework.
- **DB**: HikariCP + pgjdbc, plain JDBC, isolated in the `postgres` Maven/JPMS module.
- **JSON**: Jackson 3 (`tools.jackson.*`), used **tree-model only** (`ObjectNode`) — no reflection, so the module needs no `opens`; all Jackson exceptions are unchecked in 3.x, so no wrapper boilerplate either.
- **Search**: a generic `SearchIndex` core interface backed in production by a thin hand-rolled Elasticsearch REST client over `java.net.http.HttpClient`.

Runtime third-party jars: `HikariCP` 7.x, `postgresql`, Jackson 3 `jackson-{core,databind}` (plus the 2.x `jackson-annotations` jar Jackson 3 intentionally retains for package stability), and `slf4j-api` + `slf4j-jdk14` — slf4j is a hard dependency of HikariCP; the jdk14 provider routes its logging into `java.util.logging`, keeping logging inside the JDK. Hikari 7 depends on slf4j-api 2.x natively, so versions align without any pinning. Test-only: JUnit 5 and Testcontainers.

## Modules

| Maven artifact | JPMS module | Purpose |
|---|---|---|
| `jpms-server-core` | `jpms.server.core` | Notes domain, HTTP API, repository/search interfaces |
| `jpms-server-postgres-schema` | `jpms.server.postgres.schema` | Optional PostgreSQL schema SQL/bootstrap utility |
| `jpms-server-postgres` | `jpms.server.postgres` | Hikari/JDBC repository and pgjdbc dependency |
| `jpms-server-elasticsearch` | `jpms.server.elasticsearch` | Elasticsearch REST implementation for `SearchIndex` |
| `jpms-server-elasticsearch-schema` | `jpms.server.elasticsearch.schema` | Optional Elasticsearch index bootstrap utility |
| `jpms-server-app` | `jpms.server.app` | `Main`, config, and `ServiceLoader` lookup |

## Build & test

With direnv (`.envrc` → `use nix`, cached by nix-direnv), `cd` into the repo and JDK 25 + Maven are on PATH — run `direnv allow` once. Without direnv, `nix-shell` gives the same environment (shell.nix).

```sh
mvn spotless:apply   # format Java sources across the reactor
mvn verify           # compiles on the module path (JPMS enforced), checks formatting, runs all tests
```

The schema integration tests require a working Docker daemon. Testcontainers starts the
same PostgreSQL and Elasticsearch image versions used by `compose.yaml`; the first run may
download them. Fast core tests still use in-memory implementations, and `EsRestClientTest`
keeps its scripted local HTTP server for transport and failure-path coverage. During
`verify`, the app integration test also launches the packaged production module command
and exercises the complete HTTP lifecycle against both real backends.

## Run

```sh
docker compose up -d   # local Postgres + Elasticsearch (optional if you point env vars elsewhere)
./run.sh ddl           # optional: apply local test DDL from jpms-server-postgres-schema
./run.sh es-schema     # optional: upsert Elasticsearch indexes from app-provided schema
./run.sh
curl -s localhost:8080/healthz
```

The application assumes its SQL schema and search indexes already exist. The optional
`jpms-server-postgres-schema` module contains the current PostgreSQL DDL used by tests
and local setup. The optional `jpms-server-elasticsearch-schema` module owns the bundled
Elasticsearch index definitions and asks the Elasticsearch module to upsert every
declared index. Missing indexes are created with their full definition; existing indexes
receive mapping updates only, because most meaningful Elasticsearch settings are
create-time or migration concerns. `jpms-server-app` does not run either automatically.
That keeps production deployment policy outside app startup.

For local testing, `./run.sh ddl` applies the bundled PostgreSQL `schema.sql` against
`JPMS_SERVER_POSTGRES_TARGET`, `JPMS_SERVER_POSTGRES_USER`, and `JPMS_SERVER_POSTGRES_PASS`. It is an explicit command, not part of app startup.

For local testing, `./run.sh es-schema` upserts the app's Elasticsearch index definitions
against `JPMS_SERVER_ELASTICSEARCH_TARGET` using the same Elasticsearch auth environment
as the runtime provider. The sample app currently declares two indexes: `notes` for
full note search and `note_suggestions` for a separate suggestion-oriented use case.

`run.sh` launches fully modular — everything on the module path:

```sh
java --module-path app/target/classes:app/target/modules \
  -m jpms.server.app/jpms.server.Main
```

### Linked runtime image

Build a platform-specific, self-contained runtime image with `jlink`:

```sh
./build-image.sh
app/target/jpms-server/bin/jpms-server
```

The resulting launcher uses the same environment variables as `run.sh` and does not require a
separately installed JDK. PostgreSQL JDBC currently exposes an automatic module rather than an
explicit descriptor, which `jlink` cannot consume. The build therefore compiles
`app/src/jlink/postgresql/module-info.java` and injects it into a staged copy of the driver; the
Maven dependency remains unchanged. `--bind-services` includes the PostgreSQL, Elasticsearch, and
logging providers selected through `ServiceLoader`. The schema CLIs remain separate deployment
commands and are not included in the application image.

### Poking the API — `api`

Wraps every endpoint so you never hand-write curl; `BASE_URL` overrides the target (default `http://localhost:8080`).

```sh
./api health
./api create "Groceries" "buy milk and eggs"
./api list
./api get 1
./api search milk        # optional second arg: size
./api reindex
./api delete 1
./api smoke              # end-to-end verification against the running server
```

`smoke` runs the full lifecycle — create → get → search (polling, since real ES makes docs searchable on refresh ~1s) → reindex → delete → verify gone — printing ok/FAIL per step and exiting non-zero on any failure.

### Configuration (env vars)

| Var | Default |
|---|---|
| `JPMS_SERVER_APP_PORT` | `8080` |
| `JPMS_SERVER_POSTGRES_TARGET` | `localhost:5432/app` |
| `JPMS_SERVER_POSTGRES_USER` / `JPMS_SERVER_POSTGRES_PASS` | `app` / `app` |
| `JPMS_SERVER_POSTGRES_POOL_SIZE` | `10` |
| `JPMS_SERVER_ELASTICSEARCH_TARGET` | `http://localhost:9200` |
| `JPMS_SERVER_ELASTICSEARCH_API_KEY` | unset → interpreted by the Elasticsearch provider |
| `JPMS_SERVER_ELASTICSEARCH_USER` / `JPMS_SERVER_ELASTICSEARCH_PASS` | unset → interpreted by the Elasticsearch provider if no API key is set |

`JPMS_SERVER_POSTGRES_TARGET` is the Postgres location/database/query part only, for example
`localhost:5432/app` or `db.example.com:5432/app?sslmode=require`. The Postgres provider
adds `jdbc:postgresql://`. A full `jdbc:postgresql:` value is still accepted in
`JPMS_SERVER_POSTGRES_TARGET` as a compatibility escape hatch.

`JPMS_SERVER_ELASTICSEARCH_TARGET` is the search backend target. With the Elasticsearch provider,
it is the Elasticsearch base URL. The app passes the process environment to providers but
does not interpret Elasticsearch auth. `JPMS_SERVER_ELASTICSEARCH_API_KEY`,
`JPMS_SERVER_ELASTICSEARCH_USER`, and `JPMS_SERVER_ELASTICSEARCH_PASS` belong to the
Elasticsearch module.

Provider config uses module-scoped names: `JPMS_SERVER_POSTGRES_*` for the Postgres
module, `JPMS_SERVER_ELASTICSEARCH_*` for the Elasticsearch module, and
`JPMS_SERVER_APP_*` for app-owned settings. `PORT` is still accepted as a local
compatibility fallback for `JPMS_SERVER_APP_PORT`.

### Endpoints

```
GET    /healthz
POST   /notes              {"title": "...", "body": "..."}   → 201, inserts row + indexes doc
GET    /notes
GET    /notes/{id}
DELETE /notes/{id}                                           → 204, deletes row + doc
GET    /notes/_search?q=milk&size=10                         → multi_match over title^2, body
POST   /notes/_reindex                                       → _bulk reindex of all rows, reports per-item failures
```

## Design notes

### The Elasticsearch client (`elasticsearch/EsRestClient.java`)

What a thin client needs in practice, and where the traps are:

- **Transport** is `java.net.http.HttpClient`: in the JDK, keep-alive pooling for free, auth is just an `Authorization` header.
- **`_bulk` is the trap**: NDJSON body (action line + optional source line, every line newline-terminated), and **HTTP 200 does not mean success** — the client always parses `errors` and walks `items[]` for per-item error objects. Nuance it gets right: a delete of a missing doc is a 404 item *without* an error object, which real ES does not count as a failure.
- **Retries**: 429/502/503/504 and transport errors are retried with exponential backoff + jitter, honoring `Retry-After`. Safe because every operation uses explicit document ids (idempotent). Node sniffing/dead-node marking is deliberately out of scope — pointless behind a load balancer or managed endpoint.
- **Queries are JSON** (`ObjectNode`), the same thing you'd paste into Kibana to debug.

### The swap pattern

Both infrastructure edges are swapped through JPMS services — by construction, not by framework:

| Edge | Core service | Production provider | Tests |
|---|---|---|
| SQL | `NoteStoreProvider` | `jpms.server.postgres provides ... with PostgresNoteStoreProvider` | `postgres-schema` verifies the real schema and repository on Testcontainers PostgreSQL; core tests use `InMemoryNoteRepository` |
| Search | `SearchIndexProvider` | `jpms.server.elasticsearch provides ... with EsRestClientProvider` | `elasticsearch-schema` verifies both indexes on Testcontainers Elasticsearch; core tests use `InMemorySearchIndex` |
| DB schema | Backend API | `jpms-server-postgres-schema` depends on `jpms-server-postgres` and calls `PostgresSchemaApplier` directly | CLI/bootstrap concern |
| Search schema | Backend API | `jpms-server-elasticsearch-schema` depends on `jpms-server-elasticsearch` and calls `EsSchemaApplier` directly | CLI/bootstrap concern |

`Main` declares `uses` for those provider interfaces and loads exactly one provider of each type via `ServiceLoader`. The concrete implementation is chosen by which provider module is present on the runtime module path, not by importing `EsRestClient` or `PostgresNoteRepository` in the app.

`InMemorySearchIndex` (test sources) implements the query subset the app uses — `match_all`, `term`, `match` (OR semantics), `multi_match` (boosts stripped), `bool` — and throws on anything else so a test can never silently pass on an unsupported query. Extend it as your queries grow.

### JPMS

Every Maven submodule has a `module-info.java`. Core exports only its API packages and does not read SQL, Hikari, pgjdbc, or `java.net.http`; those dependencies live in the implementation modules. Schema bootstrapping is deliberately separate from the Postgres access module and is not used by app startup. The app module uses only core provider interfaces:

```java
uses jpms.server.core.db.NoteStoreProvider;
uses jpms.server.core.search.SearchIndexProvider;
```

The implementation modules bind those services with `provides ... with ...`. The Postgres module keeps `requires org.postgresql.jdbc` even without direct references so the driver is present in the resolved graph. Unit tests run on the classpath (`surefire` `useModulePath=false`) while main compilation and real launches stay on the module path.
