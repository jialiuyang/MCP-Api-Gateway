# MCP Gateway Enterprise

![MCP Gateway Enterprise](mcp-api.png)

> Enterprise-grade [Model Context Protocol](https://modelcontextprotocol.io/) gateway that auto-discovers internal microservices and exposes them as MCP tools consumable by AI clients such as Cursor, Claude Code, Claude Desktop, Codex and Windsurf.

English | [简体中文](./README.md)

---

## What is it

**MCP Gateway Enterprise (MCPG)** is a configuration-driven gateway that turns a company's existing microservice fleet into MCP tools that an AI client can call directly. No business code changes required.

Platform operators only need to:
1. Configure a service registry (Nacos / Eureka / etc.)
2. Let MCPG auto-discover services, fetch their Swagger / OpenAPI specs, and synthesize MCP tools
3. Drop the MCP endpoint URL into Cursor (or any compliant client) and start using it

## Key features

| Capability | Description |
|-----------|-------------|
| 🔌 **Multi-registry** | Nacos / Eureka out of the box; Consul / Polaris / K8s / Zookeeper stubs ready to grow |
| 📚 **Multiple OpenAPI dialects** | Swagger 2.0, OpenAPI 3.0 / 3.1 (covers Springdoc, Springfox, FastAPI, …) |
| 🤖 **Three exposure modes** | `META` (meta tools), `DIRECT_ALL` (all-in), `HYBRID` (meta + promoted operations) |
| 🛠️ **Meta tools** | `list_services` / `search_api` / `get_api_schema` / `call_api` — defeats tool-count explosion |
| ⏰ **Daily refresh** | Cron-driven Swagger re-fetch with on-demand manual refresh |
| 🔒 **Governance hooks** | SSO, audit, environment isolation, write-op tiers, rate limit — interfaces reserved, UI live |
| 🚀 **Single-jar deploy** | `mvn package` bundles the Vue console into the Spring Boot jar |

## Current status

The project is delivered in five milestones:

| Milestone | Scope | Status |
|-----------|-------|--------|
| **B1** | Project skeleton + build pipeline | ✅ Done |
| **B2** | OpenAPI parsing + four meta tools + Cursor integration | ✅ Done |
| **B3** | Registry adapters + auto-discovery + daily refresh | ✅ Done |
| **B4** | Exposure strategies (META / HYBRID / DIRECT_ALL) + Promote | ✅ Done |
| **B5** | High-fidelity UI (audit / policies / health / settings) + demo seed + docs | ✅ Done |

## Tech stack

- **Backend**: Java 17 + Spring Boot 3.3 + Spring Data JPA + H2 (dev) / MySQL (prod)
- **MCP**: [`io.modelcontextprotocol.sdk:mcp`](https://github.com/modelcontextprotocol/java-sdk)
- **OpenAPI**: [`io.swagger.parser.v3:swagger-parser`](https://github.com/swagger-api/swagger-parser)
- **Registries**: `nacos-client`, `eureka-client`
- **Frontend**: Vue 3 + Vite + TypeScript + Element Plus + Pinia
- **Build**: Maven multi-module + frontend-maven-plugin (one-click build)

## Quick start

### Prerequisites

- JDK 17+
- Maven 3.9+
- Internet access to Maven Central (Node 20 will be downloaded automatically)
- *Optional*: a reachable Nacos / Eureka instance for service discovery

### Build and run

```bash
git clone https://github.com/your-org/mcp-gateway-enterprise.git
cd mcp-gateway-enterprise
mvn clean package           # Builds both backend and frontend
java -jar mcpg-web/target/mcpg-web.jar
```

Open:

- Console UI:    <http://localhost:8088/>
- API docs:      <http://localhost:8088/swagger-ui.html>
- MCP endpoint (Cursor 0.45+):  `http://localhost:8088/mcp` (Streamable HTTP)
- MCP endpoint (older clients): `http://localhost:8088/mcp/sse` (HTTP+SSE, add `"type": "sse"` in `mcp.json`)

> Default port is **8088** (avoids Nacos defaults 8080/8848). Override with `MCPG_PORT=xxxx`.

> Skip the frontend rebuild for backend-only iteration: `mvn package -Dskip.frontend=true`

### Connect Cursor

In `~/.cursor/mcp.json` (or the workspace-level `.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "mcpg-local": {
      "url": "http://localhost:8088/mcp"
    }
  }
}
```

A 5-minute walkthrough (Petstore-based demo, prompts and screenshots) is in
[`docs/demo-cursor.md`](./docs/demo-cursor.md).

## Module layout

```
mcp-gateway-enterprise/
├── mcpg-core/             # SPI interfaces and domain models (no Spring)
├── mcpg-parser/           # OpenAPI parser implementations
├── mcpg-registry/         # Registry adapters (incl. four stubs)
├── mcpg-server/           # MCP server: meta tools, HTTP/SSE endpoint
├── mcpg-web/              # Spring Boot main app, REST API, JPA, final jar
├── mcpg-stdio-bridge/     # Stdio bridge CLI for stdio-only MCP clients
└── mcpg-ui/               # Vue 3 console; bundled into the jar by Maven
```

See [ARCHITECTURE.md](./ARCHITECTURE.md) for the full architecture (4 Mermaid diagrams);
visual standalone versions: [docs/architecture.html](./docs/architecture.html) (dark hero) and
[docs/flow.html](./docs/flow.html) (user-side runtime call chain).

## Design decisions

See [docs/design-decisions.md](./docs/design-decisions.md). Topics include:

- Why a meta-tool model rather than direct exposure
- Why the Spring Boot app does not speak stdio directly
- How "read-only replica" data sources fit into the governance model
- Trade-offs of the three exposure strategies

## Contributing

Please read [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

[Apache License 2.0](./LICENSE)
