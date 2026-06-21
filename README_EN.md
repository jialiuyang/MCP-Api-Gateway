# MCP Gateway Enterprise

<p align="center">
  <a href="https://github.com/jialiuyang/MCP-Api-Gateway/actions/workflows/build.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/jialiuyang/MCP-Api-Gateway/build.yml?branch=main&label=build&logo=github" alt="Build">
  </a>
  <a href="https://github.com/jialiuyang/MCP-Api-Gateway/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/jialiuyang/MCP-Api-Gateway?color=blue" alt="License">
  </a>
  <a href="https://github.com/jialiuyang/MCP-Api-Gateway/commits/main">
    <img src="https://img.shields.io/github/last-commit/jialiuyang/MCP-Api-Gateway" alt="Last commit">
  </a>
  <img src="https://img.shields.io/github/languages/code-size/jialiuyang/MCP-Api-Gateway" alt="Code size">
</p>

<p align="center">
  <a href="https://github.com/jialiuyang/MCP-Api-Gateway/stargazers">
    <img src="https://img.shields.io/github/stars/jialiuyang/MCP-Api-Gateway?style=social" alt="Stars">
  </a>
  <a href="https://github.com/jialiuyang/MCP-Api-Gateway/network/members">
    <img src="https://img.shields.io/github/forks/jialiuyang/MCP-Api-Gateway?style=social" alt="Forks">
  </a>
  <a href="https://github.com/jialiuyang/MCP-Api-Gateway/issues">
    <img src="https://img.shields.io/github/issues/jialiuyang/MCP-Api-Gateway" alt="Issues">
  </a>
  <a href="https://github.com/jialiuyang/MCP-Api-Gateway/pulls">
    <img src="https://img.shields.io/github/issues-pr/jialiuyang/MCP-Api-Gateway" alt="PRs">
  </a>
  <img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg" alt="PRs Welcome">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-007396?logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=spring&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white" alt="Vue 3">
  <img src="https://img.shields.io/badge/MCP-2025--03--26-orange" alt="MCP Protocol">
  <img src="https://img.shields.io/badge/docker-ready-2496ED?logo=docker&logoColor=white" alt="Docker">
</p>

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
| 🚀 **Single jar / single image** | `mvn package` bundles the Vue console into the Spring Boot jar; `docker compose up` and you are done |

## Current status

The project is delivered in five milestones:

| Milestone | Scope | Status |
|-----------|-------|--------|
| **B1** | Project skeleton + build pipeline | ✅ Done |
| **B2** | OpenAPI parsing + four meta tools + Cursor integration | ✅ Done |
| **B3** | Registry adapters + auto-discovery + daily refresh | ✅ Done |
| **B4** | Exposure strategies (META / HYBRID / DIRECT_ALL) + Promote | ✅ Done |
| **B5** | High-fidelity UI (audit / policies / health / settings) + demo seed + docs | ✅ Done |

## 🐳 Docker one-click (recommended)

If you have Docker, a **single command** brings the whole gateway up (web console + MCP endpoint), **no JDK / Maven / Node required on the host**:

```bash
git clone https://github.com/jialiuyang/MCP-Api-Gateway.git
cd MCP-Api-Gateway
docker compose -f docker-compose.demo.yml up --build -d
```

First build: ~3–5 min (Maven deps + Node 20 + Vue bundle). Subsequent starts ≤10s.

Once running:

| Endpoint | URL |
|------|-----|
| Console UI | <http://localhost:8088/> |
| API docs (Swagger UI) | <http://localhost:8088/swagger-ui.html> |
| Health | <http://localhost:8088/actuator/health> |
| MCP endpoint (recommended) | `http://localhost:8088/mcp` |
| MCP endpoint (legacy SSE clients) | `http://localhost:8088/mcp/sse` |

Stop: `docker compose -f docker-compose.demo.yml down`.
H2 files and logs are persisted to local `./data/` and survive restarts.

> Need a slimmer production image? A single-stage `Dockerfile` is also provided; it expects you to have already run `mvn package` on the host, which fits nicely into your own CI pipeline.

## Quick start (from source)

### Prerequisites

- JDK 17+
- Maven 3.9+
- Internet access to Maven Central (Node 20 will be downloaded automatically)
- *Optional*: a reachable Nacos / Eureka instance for service discovery

### Build and run

```bash
git clone https://github.com/jialiuyang/MCP-Api-Gateway.git
cd MCP-Api-Gateway
mvn clean package           # Builds both backend and frontend
java -jar mcpg-web/target/mcpg-web.jar
```

> Default port is **8088** (avoids Nacos defaults 8080/8848). Override with `MCPG_PORT=xxxx`.

> Skip the frontend rebuild for backend-only iteration: `mvn package -Dskip.frontend=true`

## Connect Cursor

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

## Tech stack

- **Backend**: Java 17 + Spring Boot 3.5 + Spring Data JPA + H2 (dev) / MySQL (prod)
- **MCP**: [`io.modelcontextprotocol.sdk:mcp`](https://github.com/modelcontextprotocol/java-sdk)
- **OpenAPI**: [`io.swagger.parser.v3:swagger-parser`](https://github.com/swagger-api/swagger-parser)
- **Registries**: `nacos-client`, `eureka-client`
- **Frontend**: Vue 3 + Vite + TypeScript + Element Plus + Pinia
- **Build**: Maven multi-module + frontend-maven-plugin (one-click build)

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

Issues and PRs are very welcome — please read [CONTRIBUTING.md](./CONTRIBUTING.md) first.

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=jialiuyang/MCP-Api-Gateway&type=Date)](https://star-history.com/#jialiuyang/MCP-Api-Gateway&Date)

---

If this project saves you time, please consider giving it a ⭐ — it helps others discover the project.
Got questions or ideas? Open an [Issue](https://github.com/jialiuyang/MCP-Api-Gateway/issues/new) or join the [Discussions](https://github.com/jialiuyang/MCP-Api-Gateway/discussions).

## License

[Apache License 2.0](./LICENSE)
