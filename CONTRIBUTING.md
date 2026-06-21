# Contributing to MCP Gateway Enterprise

Thanks for taking the time to contribute! This document spells out the
conventions the project follows so your change can be reviewed and merged
quickly.

## Ground rules

- All new code is in **English** (identifiers, comments, log messages,
  exception text). User-facing strings exposed through the UI are translated
  via the i18n bundles.
- We follow [Conventional Commits](https://www.conventionalcommits.org/) for
  commit messages, e.g. `feat(registry): add Consul adapter`.
- Each feature change must be accompanied by tests. Skeleton placeholders are
  acceptable for milestones that have not been reached yet.

## Local setup

```bash
# 1. Backend / full build
mvn clean verify

# 2. Frontend dev server (run separately, talks to backend via /api proxy)
cd mcpg-ui
npm install
npm run dev

# 3. Backend only, skip frontend bundling
mvn -DskipTests -Dskip.frontend=true package
```

## Module layout

| Module | Responsibility |
|--------|----------------|
| `mcpg-core` | SPI interfaces and pure domain models. No Spring imports. |
| `mcpg-parser` | OpenAPI / Swagger parsers. |
| `mcpg-registry` | Service registry adapters. |
| `mcpg-server` | MCP server endpoint and meta tools. |
| `mcpg-web` | Spring Boot main app, REST API, persistence, governance hooks. |
| `mcpg-stdio-bridge` | Stdio bridge CLI. |
| `mcpg-ui` | Vue 3 console. |

A new feature usually starts in `mcpg-core` (define the interface), adds an
implementation in the appropriate sibling module, surfaces a REST endpoint in
`mcpg-web`, and finally exposes the UI in `mcpg-ui`.

## Extending the SPIs

Adding a new registry backend is a three-step process:

1. Implement `com.mcpg.core.spi.ServiceRegistryAdapter` in `mcpg-registry`.
2. Annotate the bean with `@Component` so it is picked up automatically.
3. Add the type to the UI registry-type dropdown (it appears automatically
   once the `/api/registry/adapters` endpoint reports it).

Adding a new OpenAPI dialect follows the same pattern with
`OpenApiSourceParser` in `mcpg-parser`.

## Code style

- Java: standard Spring / Google Java Style. Wrap at 120 columns.
- JavaScript / TypeScript: project uses `eslint --fix` defaults from Vite +
  Vue. Two-space indent, single quotes.
- SQL / JPQL: uppercase keywords, lowercase identifiers.

## Reporting issues

Use the issue templates in `.github/ISSUE_TEMPLATE/`. Include the milestone
in which you observed the bug (B1, B2 ...) so the maintainers can route the
report quickly.
