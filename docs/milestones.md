# Milestones

The project is delivered in five batches (B1 - B5). Each batch is committed
and tagged independently so users can adopt at the granularity that suits
them.

## B1 - Project skeleton (current)

**Goal:** establish the build pipeline and module layout. After B1 the
project builds, runs, exposes a basic dashboard and is ready to grow.

**Deliverables**

- Maven multi-module structure with the seven modules described in
  [architecture.md](./architecture.md).
- Spring Boot entry point on port 8080 with Actuator and Springdoc enabled.
- Vue 3 + Element Plus console with a side-bar layout and placeholder routes.
- Apache 2.0 license, README in zh and en, contributor docs, CHANGELOG.
- Stub registry adapters for Consul, Polaris, Kubernetes, Zookeeper so the
  UI can already advertise the roadmap.

**Acceptance**

- `mvn clean verify` is green on a clean clone.
- `java -jar mcpg-web/target/mcpg-web.jar` serves the dashboard at
  <http://localhost:8088/>.
- `/api/system/info` returns version and timestamp.

## B2 - OpenAPI parsing and meta tools

- Implement Swagger v2 and OpenAPI v3 parsers under `mcpg-parser`.
- Implement the four meta tools (`list_services`, `search_api`,
  `get_api_schema`, `call_api`) in `mcpg-server`, wired through the MCP Java
  SDK to `/mcp/sse`.
- Implement the persistence model (`mcpg_service`, `mcpg_service_spec`,
  `mcpg_tool`) and corresponding REST APIs.
- Manual Swagger import flow in the UI (paste URL, preview operations, save).
- Cursor integration page with one-click `mcp.json` snippet.

## B3 - Registry adapters and refresh scheduling

- Real `NacosRegistryAdapter` and `EurekaRegistryAdapter` implementations.
- Discovery service that ingests `DiscoveredService` lists, fetches their
  Swagger documents, and writes through to the persistence layer.
- `SwaggerRefreshScheduler` runs nightly. Manual refresh button.
- Registry config CRUD UI.

## B4 - Exposure strategies and Promote

- `ExposureMode` selector per service and a global default.
- Per-tool "promote" toggle that surfaces an operation as a first-class MCP
  tool in `HYBRID` mode.
- Stable MCP `tools/list_changed` notification when promotion or refresh
  changes the surface.

## B5 - High-fidelity UI, demo data, documentation

- High-fidelity versions of the audit, governance, health, settings pages
  (backed by stub APIs but visually finished).
- Demo seed data and a one-shot loader so the dashboard is populated on
  first start.
- Cursor demo script (`docs/demo-script.md`) and screenshots.
- Final polish pass on docs and CHANGELOG.
