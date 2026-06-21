# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### B5 - High-fidelity UI, demo data, documentation

Replaces the last four placeholder pages with end-to-end working consoles
backed by real REST endpoints, ships a one-shot demo seeder that populates
the gateway with three believable services on first boot, and lands the
Cursor demo script that walks through B1-B5 in five minutes.

#### Added

- **Audit log console** (`/audit`)
  - `AuditEventDto` + `AuditPageDto`, `AuditService` (in-memory generator
    seeded deterministically with `20260621L`, ~64 synthetic events
    covering imports, refreshes, exposure changes and MCP calls),
    `AuditController` exposing `GET /api/audit/events?outcome=&keyword=
    &page=&size=`.
  - High-fidelity Vue 3 page with status filtering, keyword search,
    color-coded outcome / HTTP-status tags, paginator, and a row-click
    drawer that lays out basic / client / detail sections via
    `el-descriptions`. Stable so the synthetic generator can be swapped
    for a persistence-backed implementation without UI churn.

- **Governance policies console** (`/policies`)
  - `PolicyEntity` (`mcpg_policy`) with severity enum + free-form
    `configJson` payload (lets new policies land without DDL), seeded
    on first boot with six representative cards
    (`write-op.guard`, `rate-limit.global`, `env.isolation`,
    `audit.retention`, `schema.redaction`, `sso.required`) across four
    categories.
  - `PolicyService.list/update`, `PolicyController`
    (`GET /api/policies`, `PUT /api/policies/{id}`); `PATCH`-style
    request body so the UI can toggle `enabled` without echoing the
    rest of the row.
  - Vue 3 card-grid page with category / status filters, inline
    enable/disable switch on every card and an Edit dialog that
    validates the JSON config before saving.

- **Tool health console** (`/health`)
  - `ToolHealthDto`, `HealthOverviewDto` with embedded latency
    histogram and 24-hour time-series.
  - `HealthService` generates the metrics deterministically from each
    tool's persistent id (success rate, P50/P95/P99, last error, last
    invocation timestamp). Numbers stay stable across refreshes which
    matters for demos and screenshots; the DTO shape is aligned with
    the planned time-series-store schema so the UI does not need to
    change later.
  - `HealthController` at `GET /api/tool-health/overview` — path is
    intentionally `/api/tool-health` (not `/api/health`) so Spring Boot
    Actuator's `/actuator/health` stays the canonical probe.
  - Vue 3 page with five KPI cards, a 24-hour call-volume line chart
    (success vs. failure overlay), a P50 latency histogram and a Top-15
    tools table with success-rate, P50/P95/P99 and last-error columns.
    Charts use `vue-echarts` with per-component imports
    (CanvasRenderer + BarChart + LineChart only) to keep bundle size
    bounded.

- **Settings console** (`/settings`)
  - `SiteSettingsEntity` (`mcpg_site_settings`, singleton row) +
    `SiteSettingsRepository` + `SiteSettingsService`. Bean-validated
    fields: site name, default environment, swagger refresh cron, max
    tools per service (10-10000), SSO toggle, audit retention days
    (1-3650), demo-mode toggle.
  - `SettingsController` (`GET / PUT /api/settings`) with PATCH-style
    body so each field is optional.
  - Vue 3 grouped-form page (General / Schedule / Governance / Demo
    sections) with inline hints, server-validated `cron` field, and a
    "last updated" header so operators can see who/when last touched
    it.

- **Demo seed runner**
  - `DemoSeedRunner` (`ApplicationRunner`) pre-populates three
    representative services on first boot:
    - `order-service` (NACOS source) — 6 operations, two pre-promoted
    - `user-service` (NACOS source) — 5 operations, two pre-promoted
    - `petstore` (MANUAL, public Swagger) — 5 operations, one
      pre-promoted
  - Idempotent (only runs when the services table is empty) and
    opt-out (`mcpg.demo.seed=false`). Enabled by default in the dev
    profile, disabled in the prod profile so production deploys never
    auto-seed.

- **Internationalization**
  - Bilingual key bundles for every new page (`audit.*`, `policies.*`,
    `health.*`, `settings.*`) added to both `zh-CN` and `en-US`.

- **Documentation**
  - `docs/demo-script.md` — 5-minute Cursor demo script that exercises
    everything B1-B5 ships (service import → exposure modes → meta
    tools → all four new consoles). Includes `screenshot: ...`
    placeholders so contributors can capture and commit screenshots to
    `docs/screenshots/`.

#### Changed

- `router/index.ts` and `AppLayout.vue` no longer render the
  `Placeholder.vue` component for the four B5 pages; the `preview`
  badges in the sidebar are gone now that the pages are real.
- README (zh-CN + en-US) marks B5 as the active milestone with all
  deliverables checked.

### B4 - Selectable exposure strategy (META / HYBRID / DIRECT_ALL)

Operators can now choose, at runtime, which set of tools the gateway
advertises to the LLM, with a live-updating UI and instant client refresh.

#### Added

- `ExposureSettingsEntity` (single-row table `mcpg_exposure_settings`) +
  `ExposureSettingsRepository`. `@PostConstruct` bootstrap inserts the
  default `HYBRID` row on first boot so the gateway is always in a
  defined state.
- `ExposureSettingsService.currentMode()` is the single source of truth
  consulted by `McpToolRegistry.list()` on every `tools/list` call.
  Composition rule per mode:
  - **META**: only the four meta tools (`list_services`, `search_api`,
    `get_api_schema`, `call_api`).
  - **HYBRID** *(default)*: meta tools + every non-deprecated row with
    `promoted=true`.
  - **DIRECT_ALL**: every non-deprecated row exposed as a first-class
    tool, meta tools dropped.
- `GET /api/exposure` returns the current mode together with live
  counters (`totalTools`, `promotedTools`, `metaToolCount`,
  `effectiveCount`) so the UI can render "will expose N tools" without
  a second round-trip.
- `PUT /api/exposure` updates the mode and publishes
  `ToolsChangedEvent`, which `ToolsChangedListener` translates into a
  broadcast `notifications/tools/list_changed` so every connected MCP
  client refreshes its tool list without restart.
- `ToolRepository` gained `countByDeprecatedFalse`,
  `countByPromotedTrueAndDeprecatedFalse`, and `findByDeprecatedFalse`
  (derived queries; no JPQL needed) for the counter / DIRECT_ALL paths.
- Tools page header now hosts an *Exposure* card showing the active
  mode (tag), human-readable rationale, four live counters, and a
  *Change mode* button that opens a radio-card dialog. The dialog
  shows per-mode summary text, marks HYBRID as **Recommended**, and
  warns operators when they pick DIRECT_ALL; switching to DIRECT_ALL
  requires an extra confirmation with the live tool count interpolated.
- Bilingual i18n bundle for every exposure string
  (`exposure.modes.*.label/summary/desc`, `exposure.stats.*`,
  `exposure.confirmDirectAll`, etc.) under both `zh-CN` and `en-US`.
- Unit tests (`McpToolRegistryExposureModeTest`) verifying the
  composition rule for all three modes, including
  deprecated-tools-are-skipped semantics in HYBRID and DIRECT_ALL.

#### Changed

- `McpToolRegistry` now takes `ExposureSettingsService` as a fourth
  constructor dependency. The list is still rebuilt on every
  `tools/list` call - mode toggles propagate without restart.

### B3 - Registry adapters + daily refresh

Service auto-discovery from external registries plus a scheduled refresh
loop so the gateway stays in sync with the source of truth.

#### Added

- Real {@code NacosServiceRegistryAdapter} backed by `nacos-client` 2.4.
  Resolves cluster credentials, namespace and group from
  `RegistryConfig`; instances are translated to candidate HTTP base URLs
  using the `secure`, `scheme` and `contextPath` metadata that Spring
  Cloud Alibaba publishes.
- Real {@code EurekaServiceRegistryAdapter} that hits `GET /eureka/apps`
  with plain HTTP + Basic auth, parsing the JSON response with Jackson.
  Deliberately avoids the Netflix `EurekaClient` dependency tree because
  the gateway only needs one-shot snapshots, not heartbeats.
- `RegistryEntity` + `RegistryRepository` for persistent registry
  configurations, with `mcpg_registry` table auto-created on first
  startup (H2 / MySQL).
- `RegistryAdapterRegistry` — central, case-insensitive lookup of all
  adapter beans (real + stubs). Detects duplicate adapter types at
  startup so two `@Component`s claiming `nacos` is a fail-fast error
  rather than a silent override.
- `RegistryDiscoveryService` — translates an entity to a transient
  `RegistryConfig`, calls the adapter, walks each `DiscoveredService`'s
  base URLs against the well-known Swagger suffix list
  (`/v3/api-docs` → `/v3/api-docs.yaml` → `/v2/api-docs` →
  `/openapi.json` → `/openapi.yaml` → `/swagger.json`) and hands off
  the first parsable URL to `ServiceImportService.importFromUrl`. The
  service tags imports with the upstream `SourceType` so the UI can
  distinguish manually-imported vs. discovered rows.
- `RegistryService` — CRUD on registry configurations and connection
  testing. Returns a sortable list of adapter types (`/api/registries/types`)
  so the UI can render the "Type" dropdown without hard-coding it.
- `RegistryController` exposing
  `/api/registries` (GET/POST), `/api/registries/{id}` (GET/PUT/DELETE),
  `/api/registries/{id}/test` and `/api/registries/{id}/discover`.
- `SwaggerRefreshScheduler` — runs the daily Swagger refresh job (cron
  configurable via `mcpg.scheduler.swagger-refresh.cron`, default
  `0 0 3 * * *`). Iterates services sequentially and swallows per-service
  failures so a single broken spec never blocks the rest of the run.
- `RegistryDiscoveryScheduler` — runs the registry polling loop with
  configurable initial delay + fixed delay (default 30s / 5m). Uses
  `fixedDelay` rather than `fixedRate` so a slow discovery never overlaps
  itself.
- Vue 3 page `Registries.vue` replacing the placeholder: full CRUD,
  inline enable/disable switch, test-connection button, discover-now
  button + a result drawer that lists per-service IMPORTED / SKIPPED
  outcomes.
- Bilingual i18n keys (`registries.*`) wired through every new label.

#### Changed

- `ImportSwaggerRequest` gained optional `sourceType` and `sourceRef`
  fields so registry-driven discovery can preserve attribution while
  still re-using `ServiceImportService`.
- `mcpg-registry` POM now declares `nacos-client` (with the `slf4j-api`
  exclusion that the binding ownership requires) and `jackson-databind`
  for Eureka parsing.

### UI - Internationalization

- Added Simplified Chinese / English bilingual support for the web console.
  Default locale falls back to the operator's browser language (Chinese
  audiences land on `zh-CN` automatically) and the choice is persisted to
  `localStorage` so the selection survives reloads.
- Element Plus locale module (`zh-cn` / `en`) is now bound reactively to the
  active i18n locale via `<el-config-provider>`; date pickers, pagination
  and other built-in widgets follow the selected language without manual
  refresh.
- Language switcher added to the top-right of every page.
- Router titles are now i18n keys (`menu.*`) resolved at render time, so
  the sidebar / breadcrumbs / header translate without a route reload.

### B2 - OpenAPI parsing, MCP server, Cursor integration

Full B2 delivery: manual Swagger import → parsed tools in the database →
MCP server with four meta tools → Cursor can connect and drive operations.

#### Added (on top of B2.1)

- MCP transport layer, **dual transport** so we work with both modern
  (Cursor 0.45+, Claude Desktop ≥ 0.8) and older clients:
  - **Streamable HTTP** (spec 2025-03-26): `POST /mcp` for JSON-RPC,
    `GET /mcp` for server-initiated SSE notifications.
  - **HTTP+SSE** (legacy 2024-11): `GET /mcp/sse` opens the stream,
    `POST /mcp/message?sessionId=` carries inbound messages.
  Hand-rolled native implementation against Spring WebMVC's `SseEmitter`,
  deliberately independent of the MCP Java SDK so we keep full control
  over the governance hooks added in later milestones. The two transports
  share a single {@code McpDispatcher} so the protocol semantics are
  identical regardless of which entrypoint the client picks.
- Four meta tools, each backed by the database:
  - `list_services` — discovery primitive, optional environment filter.
  - `search_api` — keyword search across name, summary, description,
    path and tags.
  - `get_api_schema` — full JSON schema for one operation.
  - `call_api` — actually executes the backend operation through the
    `HttpToolInvoker`. Reserved as the future anchor for audit logs and
    write-tier confirmations.
- `HttpToolInvoker` (implements the `ToolInvoker` SPI) - translates a
  `ToolInvocation` into a real HTTP request. Handles path / query / header /
  body groups according to the schema convention enforced by
  `SchemaConverter`.
- `PromotedToolFactory` and `McpToolRegistry` so a row marked
  `promoted=true` in `mcpg_tool` is automatically exposed as a first-class
  MCP tool alongside the meta tools (HYBRID exposure mode).
- `ToolsChangedEvent` + `ToolsChangedListener` so that imports, refreshes,
  deletions and promotion toggles fire `notifications/tools/list_changed`
  to every connected MCP client without restarting the gateway.
- Vue 3 pages (replacing the B1 placeholders):
  - **Services** - list, search, import dialog, refresh, delete.
  - **Tools** - list, search, promote toggle, side drawer with full schema.
  - **Cursor Integration** - shows the SSE endpoint, one-click `mcp.json`
    config for both SSE and stdio transports, walkthrough of the four meta
    tools.
- Dashboard live counters (services, tools, promoted).
- `docs/demo-cursor.md` step-by-step demo against the public Petstore
  Swagger so the team can reproduce the Cursor integration in 5 minutes.

### B2.1 - OpenAPI parsing, persistence, manual import

Foundation for B2: a working "paste a Swagger URL → tools land in the DB"
pipeline that the MCP meta-tools (B2.2) read from.

#### Added

- JPA persistence layer: `mcpg_service`, `mcpg_service_spec`, `mcpg_tool` with
  matching repositories. Schema is auto-generated on startup via
  `spring.jpa.hibernate.ddl-auto=update`.
- `OpenApiV3Parser` and `OpenApiV2Parser` beans backed by swagger-parser, with
  shared conversion logic in `OpenApiParsingSupport`. Both dialects produce
  the same normalized `ParsedSpec` so downstream code is dialect-agnostic.
- `SchemaConverter` that flattens OpenAPI parameters (path/query/header/body)
  into a single JSON Schema object, ready to be used as MCP `inputSchema`.
- `ParserRegistry` for SPI-based parser selection by content sniffing.
- `SwaggerFetcher` with friendly fallbacks to well-known suffixes
  (`/v3/api-docs`, `/openapi.json`, `/swagger.json`) when the user hands us
  the Swagger UI page instead of the spec itself.
- `ServiceImportService` orchestrating the full "fetch → parse → upsert →
  sync tools" flow inside a single transaction, with idempotent semantics:
  re-importing the same spec preserves the `promoted` flag and updates rows
  in place rather than rebuilding.
- `ToolSyncService` with merge-by-`tool_name` semantics so that re-importing
  a spec never duplicates rows and gracefully removes operations deleted
  upstream.
- `ToolNaming` utility: deterministic `service__operationId` tool names and
  initial risk-level inference (READ / WRITE_LOW / WRITE_HIGH) based on HTTP
  method and keyword heuristics.
- REST API: `GET/DELETE /api/services`, `POST /api/services/import-swagger`,
  `POST /api/services/{id}/refresh`, `GET /api/tools`, `GET /api/tools/{id}`,
  `POST /api/tools/{id}/promote`.
- `GlobalExceptionHandler` for a stable error envelope across the SPA.
- Unit tests for both parsers and an end-to-end Spring Boot test of the
  import flow against in-memory H2.

### B1 - Project skeleton

Initial commit. Establishes the multi-module Maven structure, Spring Boot
entry point, Vue 3 console scaffold, and the one-click build pipeline that
bundles the SPA into a single jar.

#### Added

- Maven multi-module layout: `mcpg-core`, `mcpg-parser`, `mcpg-registry`,
  `mcpg-server`, `mcpg-web`, `mcpg-stdio-bridge`.
- Vue 3 + Element Plus console (`mcpg-ui`) with side-bar layout and routing
  to placeholder pages for upcoming milestones.
- frontend-maven-plugin wiring that downloads Node automatically and produces
  a single runnable jar.
- Apache 2.0 license, README in both Simplified Chinese and English,
  CONTRIBUTING guide.
- SPI interfaces (`ServiceRegistryAdapter`, `OpenApiSourceParser`,
  `ToolInvoker`) and the shared domain model (`DiscoveredService`,
  `ParsedSpec`, `ParsedOperation`, `ToolInvocation`, `ToolInvocationResult`).
- Stub adapters for Consul, Polaris, Kubernetes Service and Zookeeper, so the
  UI can already advertise the roadmap.
- `/api/system/info` endpoint plus Spring Boot Actuator health/info/metrics.

#### Notes

- The MCP endpoint (`/mcp/sse`) is wired in `mcpg-server` but the meta tools
  are delivered in B2. Until then the endpoint will return an empty tool list.
- The `mcpg-stdio-bridge` module ships as a regular jar with `Main-Class`
  set in `MANIFEST.MF`. Fat-jar packaging (currently disabled because the
  `maven-shade-plugin` default transformer inherited from
  `spring-boot-starter-parent` conflicts with our `ManifestResourceTransformer`)
  will be re-enabled in B2 once the bridge has real dependencies to bundle.
- Default port changed from 8080 to **8088** to avoid collision with Nacos.
  Override with the `MCPG_PORT` environment variable.
