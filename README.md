# MCP Gateway Enterprise

<p align="center">
  <a href="https://github.com/jialiuyang/MCP-Api-Gateway/actions/workflows/build.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/jialiuyang/MCP-Api-Gateway/build.yml?branch=main&label=build&logo=github" alt="Build">
  </a>
  <img src="https://img.shields.io/github/languages/code-size/jialiuyang/MCP-Api-Gateway" alt="Code size">
</p>

<p align="center">

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

[English](./README_EN.md) | 简体中文

---

## 它是什么

**MCP Gateway Enterprise（简称 MCPG）** 是一个配置驱动的企业级 MCP 网关，让公司无需写一行业务代码，就能把全公司的微服务接入 Cursor 等 AI 编程客户端。

平台运维同学只需在 Web 控制台做几件事：
1. 配置服务注册中心（Nacos / Eureka 等）
2. MCPG 自动发现服务、抓取 Swagger / OpenAPI、生成 MCP 工具
3. 把生成的 MCP 接入地址配置到 Cursor 即可开始使用

## 核心特性

| 能力 | 描述 |
|------|------|
| 🔌 **多注册中心支持** | Nacos / Eureka 开箱即用；Consul / Polaris / K8s / ZK 占位，可按需扩展 |
| 📚 **多种 OpenAPI 格式** | Swagger 2.0、OpenAPI 3.0 / 3.1（覆盖 Springdoc、Springfox、FastAPI 等主流框架） |
| 🤖 **三档暴露策略** | `META`（元工具）、`DIRECT_ALL`（全部直暴露）、`HYBRID`（混合 + Promote 提升） |
| 🛠️ **元工具搜索** | `list_services` / `search_api` / `get_api_schema` / `call_api` 四件套，解决大规模工具列表爆炸问题 |
| ⏰ **定时刷新** | 每日定时拉取 Swagger，保持工具与接口同步；支持手动立即刷新 |
| 🔒 **治理预留** | SSO、审计、环境隔离、写操作分级、限流——接口预留，UI 已上线 |
| 🚀 **单 Jar / 单镜像部署** | `mvn package` 把 Vue 前端打入 Spring Boot Jar；`docker compose up` 即开即用 |

## 当前进展

本项目按 5 个里程碑逐步交付：

| 里程碑 | 内容 | 状态 |
|--------|------|------|
| **B1** | 工程骨架 + 构建链路 | ✅ 已完成 |
| **B2** | OpenAPI 解析 + 4 个元工具 + Cursor 接入 | ✅ 已完成 |
| **B3** | 注册中心适配 + 自动发现 + 每日刷新 | ✅ 已完成 |
| **B4** | 暴露策略（META / HYBRID / DIRECT_ALL）+ Promote 机制 | ✅ 已完成 |
| **B5** | 高保真 UI（审计 / 策略 / 健康 / 设置）+ Demo 种子 + 完整文档 | ✅ 已完成 |

## 🐳 Docker 一键启动（推荐）

只要装了 Docker，**一行命令**即可拉起完整服务（Web 控制台 + MCP 端点），**无需在宿主机安装 JDK / Maven / Node**：

```bash
git clone https://github.com/jialiuyang/MCP-Api-Gateway.git
cd MCP-Api-Gateway
docker compose -f docker-compose.demo.yml up --build -d
```

首次构建约 3–5 分钟（含 Maven 依赖 + Node 20 + Vue 打包）；之后启动 ≤10s。

启动后访问：

| 入口 | URL |
|------|-----|
| 控制台 UI | <http://localhost:8088/> |
| 后端 API 文档（Swagger UI） | <http://localhost:8088/swagger-ui.html> |
| 健康检查 | <http://localhost:8088/actuator/health> |
| MCP 接入端点（推荐） | `http://localhost:8088/mcp` |
| MCP 接入端点（SSE 旧客户端） | `http://localhost:8088/mcp/sse` |

停止：`docker compose -f docker-compose.demo.yml down`。
数据（H2 文件、日志）会落到本地 `./data/` 目录，重启后保留。

> 想跑生产体积更小的镜像？仓库根目录还提供单阶段 `Dockerfile`（要求宿主机先 `mvn package`），适合放进自家 CI/CD 流水线。

## 快速开始（源码方式）

### 前置要求

- JDK 17+
- Maven 3.9+
- 网络可访问 Maven Central（前端依赖会由 Maven 自动下载 Node 20 与依赖）
- （可选）本地或可访问的 Nacos / Eureka，用于服务自动发现

### 构建并运行

```bash
git clone https://github.com/jialiuyang/MCP-Api-Gateway.git
cd MCP-Api-Gateway
mvn clean package           # 一次性构建后端 + 前端，产物 mcpg-web/target/mcpg-web.jar
java -jar mcpg-web/target/mcpg-web.jar
```

> 默认端口为 **8088**（避开 Nacos 默认占用的 8080/8848）。如需修改，设置环境变量 `MCPG_PORT=xxxx`。

> 仅做后端开发想跳过前端打包，加参数：`mvn package -Dskip.frontend=true`

## Cursor 接入示例

在 Cursor 的 `~/.cursor/mcp.json`（或工作区 `.cursor/mcp.json`）中添加：

```json
{
  "mcpServers": {
    "mcpg-local": {
      "url": "http://localhost:8088/mcp"
    }
  }
}
```

完整 5 分钟 demo（含截图与典型 Prompt）见 [`docs/demo-cursor.md`](./docs/demo-cursor.md)。

## 技术栈

- **后端**：Java 17 + Spring Boot 3.5 + Spring Data JPA + H2 (开发) / MySQL (生产)
- **MCP**：[`io.modelcontextprotocol.sdk:mcp`](https://github.com/modelcontextprotocol/java-sdk)
- **OpenAPI**：[`io.swagger.parser.v3:swagger-parser`](https://github.com/swagger-api/swagger-parser)
- **注册中心**：`nacos-client`、`eureka-client`
- **前端**：Vue 3 + Vite + TypeScript + Element Plus + Pinia
- **构建**：Maven 多模块 + frontend-maven-plugin（前端一键打包）

## 工程结构

```
mcp-gateway-enterprise/
├── mcpg-core/             # SPI 接口、领域模型（无 Spring 依赖）
├── mcpg-parser/           # OpenAPI 解析实现
├── mcpg-registry/         # 注册中心适配实现（含 4 个占位空壳）
├── mcpg-server/           # MCP Server 实现（元工具、HTTP/SSE 端点）
├── mcpg-web/              # Spring Boot 主应用、REST API、JPA、最终打包
├── mcpg-stdio-bridge/     # Stdio 桥接 CLI（用于只支持 stdio 的 MCP 客户端）
└── mcpg-ui/               # Vue 3 前端，由 Maven 自动打包并内嵌到 jar
```

详细架构图请见 [ARCHITECTURE.md](./ARCHITECTURE.md)（含 4 张 Mermaid 视图）；
高清版可视化版本：[docs/architecture.html](./docs/architecture.html)（深色封面）与 [docs/flow.html](./docs/flow.html)（用户调用链路简版）。

## 设计决策

详见 [docs/design-decisions.md](./docs/design-decisions.md)。其中讨论了：

- 为什么使用元工具模式而不是直接暴露所有接口
- 为什么 Spring Boot 不直接做 stdio，需要单独的桥接 CLI
- 为什么把生产数据库默认为只读副本
- 三档暴露策略的取舍

## 贡献

欢迎提 Issue / PR！请先阅读 [CONTRIBUTING.md](./CONTRIBUTING.md)。

如果这个项目对你有帮助，欢迎点个 ⭐ Star 让更多人看到。
有问题或建议？提个 [Issue](https://github.com/jialiuyang/MCP-Api-Gateway/issues/new) 或参与 [Discussions](https://github.com/jialiuyang/MCP-Api-Gateway/discussions)。

## 协议

[Apache License 2.0](./LICENSE)
