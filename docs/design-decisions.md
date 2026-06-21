# Design Decisions

This document records the non-obvious choices behind MCP Gateway Enterprise.
Each decision states the context, the alternatives considered and the
reasoning. Decisions are intentionally written as snapshots so future
contributors can revisit them when conditions change.

## 1. Java 17 + Spring Boot 3 + Vue 3 + Element Plus

**Context.** The target audience is enterprise platform teams in China. The
project must be approachable, easy to host, and fit existing toolchains.

**Decision.** Adopt Java 17 LTS, Spring Boot 3.3, Vue 3 with TypeScript and
Element Plus.

**Why not...?**

- *Java 21.* Most enterprises are still standardizing on 17. 17 covers virtual
  threads (preview) sufficiently for our workload and avoids unnecessary
  upgrade friction.
- *Quarkus / Micronaut.* Faster startup, but the Spring ecosystem is more
  recognizable to the user base and offers richer registry / actuator
  integration.
- *React / Ant Design.* The team writing this gateway prefers Vue, and
  Element Plus offers a one-shop UI kit tuned to back-office consoles.

## 2. Meta-tool default with three exposure modes

**Context.** A single gateway can aggregate hundreds of services with
thousands of operations. Naively turning each operation into an MCP tool
floods the LLM's context with tool descriptions and degrades selection
quality.

**Decision.** Default to `META`: only the four meta tools
(`list_services`, `search_api`, `get_api_schema`, `call_api`) are exposed.
Operators can switch to `DIRECT_ALL` (every operation surfaced as its own
tool) for demos or small deployments, or `HYBRID` to selectively promote
high-traffic operations.

**Trade-offs.**

- Meta tools cost the LLM an extra round trip for discovery.
- Direct exposure is faster but only viable below ~100 tools.
- Hybrid mode lets us discover the "fat tail" of operations that benefit from
  promotion, but adds operator workload to curate the promoted list.

## 3. Spring Boot speaks HTTP/SSE; a separate CLI bridges stdio

**Context.** Some MCP clients (older Cursor releases, sandbox tools) only
support stdio transport. Spring Boot occupies stdin/stdout for its own logs,
which makes embedding stdio support in the main process fragile.

**Decision.** The main application exposes `/mcp/sse` only. A small
`mcpg-stdio-bridge` CLI (single fat jar) is shipped alongside; it accepts
JSON-RPC on stdin and proxies it to the central endpoint.

**Side-effects.**

- The bridge can be configured with `--endpoint` and `--token`, which lets a
  single backend service many isolated developers.
- The bridge stays minimal so it remains stable as the protocol evolves.

## 4. Adapters are pluggable; stubs ship for upcoming backends

**Context.** Different enterprises use different service registries. We want
to ship something useful out of the box without limiting future support.

**Decision.** Define a single `ServiceRegistryAdapter` SPI. Nacos and Eureka
ship as full implementations. Consul, Polaris, Kubernetes and Zookeeper
ship as stub adapters that surface in the UI dropdown but throw
`UnsupportedOperationException` if engaged. Users can see the roadmap and
plan migrations.

## 5. Daily refresh, not real-time webhook

**Context.** OpenAPI specifications change with releases, but most teams
deploy on the order of hours / days. Real-time webhook integration with
every API portal is expensive to build for limited benefit.

**Decision.** A cron-driven scheduler refreshes specs once per day (default
03:00 server time, configurable). Users can also click "refresh now" in the
UI for any individual service.

**When to revisit.** If a company has a centralized API gateway that emits
events on spec change, building a push-based listener becomes economical.

## 6. Production data access is governed, not blocked

**Context.** It is tempting to forbid the gateway from touching production
entirely. That would, however, eliminate one of the most valuable use cases
(production debugging from Cursor).

**Decision.** The governance layer (delivered after B5) lets each tool
declare an allowed environment set and a risk grade. Production access is
restricted to read-only operations against pre-defined replicas and
observability surfaces; write operations require explicit approval. The
gateway never grants new capabilities — it merely lets the LLM use what the
operator already has.

## 7. One jar to rule them all

**Context.** The audience does not necessarily have a frontend toolchain.

**Decision.** `frontend-maven-plugin` downloads a known-good Node version and
builds the Vue console during `mvn package`. The build copies `mcpg-ui/dist`
into the Spring Boot jar's `static/` directory. The resulting jar is the
only deployable artifact for the standalone setup.

**Escape hatch.** `mvn package -Dskip.frontend=true` skips the UI rebuild for
backend-only iteration.

## 8. H2 in dev, MySQL in prod

**Context.** Demo experience matters: contributors should not have to
provision a database to run the gateway locally. Production deployments need
a real RDBMS.

**Decision.** Spring profile `dev` (default) writes to an on-disk H2 file.
Profile `prod` connects to MySQL via standard `MCPG_DB_*` environment
variables. JPA `ddl-auto=update` is enabled in dev; `validate` in prod plus
Flyway-managed migrations (added in B3 when the schema first stabilizes).
