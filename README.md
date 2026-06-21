# MCP Gateway Enterprise

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
| 🚀 **单 Jar 部署** | `mvn package` 一行命令，把 Vue 前端打入 Spring Boot Jar |

## 当前进展

本项目按 5 个里程碑逐步交付：

| 里程碑 | 内容 | 状态 |
|--------|------|------|
| **B1** | 工程骨架 + 构建链路 | ✅ 已完成 |
| **B2** | OpenAPI 解析 + 4 个元工具 + Cursor 接入 | ✅ 已完成 |
| **B3** | 注册中心适配 + 自动发现 + 每日刷新 | ✅ 已完成 |
| **B4** | 暴露策略（META / HYBRID / DIRECT_ALL）+ Promote 机制 | ✅ 已完成 |
| **B5** | 高保真 UI（审计 / 策略 / 健康 / 设置）+ Demo 种子 + 完整文档 | ✅ 已完成 |

## 技术栈

- **后端**：Java 17 + Spring Boot 3.3 + Spring Data JPA + H2 (开发) / MySQL (生产)
- **MCP**：[`io.modelcontextprotocol.sdk:mcp`](https://github.com/modelcontextprotocol/java-sdk)
- **OpenAPI**：[`io.swagger.parser.v3:swagger-parser`](https://github.com/swagger-api/swagger-parser)
- **注册中心**：`nacos-client`、`eureka-client`
- **前端**：Vue 3 + Vite + TypeScript + Element Plus + Pinia
- **构建**：Maven 多模块 + frontend-maven-plugin（前端一键打包）

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.9+
- 网络可访问 Maven Central（前端依赖会由 Maven 自动下载 Node 20 与依赖）
- （可选）本地或可访问的 Nacos / Eureka，用于服务自动发现

### 构建并运行

```bash
git clone https://github.com/your-org/mcp-gateway-enterprise.git
cd mcp-gateway-enterprise
mvn clean package           # 一次性构建后端 + 前端，产物 mcpg-web/target/mcpg-web.jar
java -jar mcpg-web/target/mcpg-web.jar
```

启动完成后访问：

- 控制台 UI：<http://localhost:8088/>
- 后端 API 文档：<http://localhost:8088/swagger-ui.html>
- MCP 接入地址（Cursor 0.45+）：`http://localhost:8088/mcp`（Streamable HTTP）
- MCP 接入地址（旧版客户端）：`http://localhost:8088/mcp/sse`（HTTP+SSE，需在 `mcp.json` 加 `"type": "sse"`）

> 默认端口为 **8088**（避开 Nacos 默认占用的 8080/8848）。如需修改，设置环境变量 `MCPG_PORT=xxxx`。

> 仅做后端开发想跳过前端打包，加参数：`mvn package -Dskip.frontend=true`

### Cursor 接入示例

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

请阅读 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## 协议

[Apache License 2.0](./LICENSE)
