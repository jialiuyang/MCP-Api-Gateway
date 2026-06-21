# Architecture

## High-level diagram

```
                    +-------------------------------+
                    |        Web Console (Vue)      |
                    +---------------+---------------+
                                    | HTTPS / REST  (default :8088)
                                    v
+-----------------------------------------------------------------+
|                  Spring Boot application (mcpg-web)             |
|                                                                 |
|  +------------------+ +------------------+ +-----------------+  |
|  | Discovery        | | Parser           | | MCP Server      |  |
|  | + registry SPI   | | + OpenAPI v2/v3  | | + HTTP/SSE      |  |
|  | + Nacos / Eureka | | + plugin SPI     | | + meta tools    |  |
|  +--------+---------+ +---------+--------+ +--------+--------+  |
|           |                     |                   |           |
|           +---------- shared -- + ------------------+           |
|                                  |                              |
|                       +----------v----------+                   |
|                       | Invoker (HTTP/REST) |                   |
|                       +---------------------+                   |
|                                                                 |
|  +-----------------------------------------------------------+  |
|  | Persistence (Spring Data JPA) -> H2 / MySQL                |  |
|  +-----------------------------------------------------------+  |
|                                                                 |
|  Reserved hooks (UI live, backend stubs):                       |
|   - Auth / SSO     - Audit log      - Rate limit                |
|   - Env isolation  - Risk grading   - LLM description boost     |
+-----------------+---------------------------+-------------------+
                  |                           |
                  v                           v
        +-------------------+        +-----------------------+
        | Service registry  |        | Internal microservices|
        | Nacos / Eureka    |        | (HTTP / REST)         |
        +-------------------+        +-----------------------+

        Optional: stdio bridge CLI (mcpg-stdio-bridge) for stdio MCP clients
```

## Module boundaries

- **`mcpg-core`** — Pure Java. Defines the SPI contracts that the rest of the
  system extends. New backends are integrated by depending only on this
  module.
- **`mcpg-parser`** — Realizes `OpenApiSourceParser`. Currently bundles
  Swagger 2 and OpenAPI 3 parsers, both backed by `swagger-parser`.
- **`mcpg-registry`** — Realizes `ServiceRegistryAdapter`. Nacos and Eureka
  are real adapters; Consul, Polaris, Kubernetes and Zookeeper are stubs.
- **`mcpg-server`** — Owns the four meta tools and the HTTP/SSE endpoint.
  Holds no business state of its own; defers to persistence via core
  services.
- **`mcpg-web`** — The deployable Spring Boot artifact. Wires every module
  together, hosts REST controllers, governance hooks, scheduling and bundles
  the SPA.
- **`mcpg-stdio-bridge`** — Tiny CLI for MCP clients that only speak stdio.
  Forwards JSON-RPC frames to the central HTTP/SSE endpoint.
- **`mcpg-ui`** — Vue 3 SPA, built by Maven and copied into the jar.

## Request flow: an LLM invokes a backend operation

```
Cursor                       MCPG (HTTP/SSE)              Backend microservice
  |                                |                            |
  |--- tools/list --------------->|                            |
  |<-- 4 meta tools --------------|                            |
  |                                |                            |
  |--- call_api(search_api, ..) ->|                            |
  |    search "order status"       | (DB lookup, no backend call)
  |<-- candidate tool list --------|                            |
  |                                |                            |
  |--- call_api(getOrder, ..) --->|--- HTTP GET /orders/123 -->|
  |                                |<-- 200 OK + JSON ----------|
  |<-- result -----------------    |                            |
```

The same flow works for direct exposure mode: instead of `call_api` the LLM
calls the operation tool directly.

## Persistence model

Entities (created in B2 / B3):

| Table | Purpose |
|-------|---------|
| `mcpg_registry_config` | Stored registry connections |
| `mcpg_service` | One row per discovered or imported service |
| `mcpg_service_spec` | Cached OpenAPI document for a service |
| `mcpg_tool` | One row per generated MCP tool |
| `mcpg_call_log` | Audit row per invocation (reserved) |
| `mcpg_policy` | Governance rules (reserved) |

## Why the meta-tool model is the default

When a single MCPG instance fronts hundreds of services with thousands of
operations, exposing every operation as a first-class MCP tool overwhelms the
LLM (tool list bloat, context cost, decreased selection accuracy). The four
meta tools (`list_services`, `search_api`, `get_api_schema`, `call_api`) keep
the surface tiny and let the LLM discover dynamically.

`DIRECT_ALL` and `HYBRID` modes are retained for demos and for the small set
of high-frequency operations where direct exposure outperforms search.
