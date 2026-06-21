# 技术分享：MCP —— 从协议本质到企业落地

> **分享时长**：40 分钟讲 + 15 分钟答疑
> **目标听众**：日常使用 Cursor / Claude Code / Windsurf 等 AI 编程工具的研发同学；对架构、协议、AI 工程化感兴趣的同事
> **配套 Demo**：[MCP Gateway Enterprise](https://github.com/jialiuyang/MCP-Api-Gateway) —— 本仓库
> **作者**：jialiuyang

---

## 0. 议程

| # | 段落 | 时长 | 关键问题 |
|---|------|------|----------|
| 1 | 起点：Cursor 为什么忽然"懂"我们的系统了 | 5 min | 为什么这是一件该被关注的事 |
| 2 | MCP 到底是什么 | 7 min | 协议定位、生态全景 |
| 3 | 协议深度拆解 | 10 min | JSON-RPC、Capabilities、三大原语、双传输 |
| 4 | "我手写一个 RPC 不行吗" —— 协议价值的硬核论证 | 5 min | 标准化、组合、互操作 |
| 5 | 企业现实：千服务 × 万接口 × LLM | 3 min | 工具列表爆炸、安全、治理 |
| 6 | 解法：MCP Gateway Enterprise 架构与设计决策 | 8 min | 元工具模式、暴露策略、注册中心、刷新 |
| 7 | 治理与生产化 | 3 min | 审计、策略、健康、SSO、写操作分级 |
| 8 | 现场 Demo：5 分钟从 0 到 Cursor 跑通公司接口 | 5 min | 看得见、摸得着 |
| 9 | 未来 & 风险 | 2 min | spec 演进、A2A、MCP-UI、sampling |
| 10 | 学习路径与资源（讲义自取） | 2 min | 自学清单 |

---

## 1. 起点：Cursor 为什么忽然"懂"我们的系统了

### 1.1 一个真实场景

以前在 Cursor 里让 AI 写订单接口的调用：

```
你：帮我写一段调用订单服务取消订单的代码
AI：（猜一个 URL）curl -X POST /api/orders/cancel ...
你：错了，我们的接口是 /v2/orders/{id}/cancel，参数也不一样
AI：好的我再写……
```

接入 MCP 之后：

```
你：帮我取消订单 12345
AI：（自动 list_services → search_api "cancel order" → get_api_schema → call_api）
    已调用 order-service.cancelOrder(id=12345)，返回 200，订单状态：CANCELLED
```

差异在于：**AI 不再"猜"我们的 API，而是真的"看见"了它**。

### 1.2 这件事为什么值得专门讲一次

- **行业拐点**：2024-11 Anthropic 发布 MCP 0.1，2025 年内 OpenAI（Codex）、Google（Gemini）、Microsoft（Copilot Studio）相继宣布支持 —— 这是 LLM 时代第一个被三大厂同时押注的接口协议
- **架构师视角**：MCP 是从"LLM 调用工具"这个高频动作里抽出的最小通用协议，跟 1990s 的 HTTP、2010s 的 gRPC 是同一类东西 —— 一旦标准化，生态会井喷
- **企业研发视角**：公司已有几百个微服务、几千个 Swagger 接口，**怎么让 AI 工具"安全地"用上**？这恰好是我们 demo 要解决的事

> 一句话总结：MCP 不是某个公司的产品，是给 LLM 用的 USB 接口标准。

---

## 2. MCP 到底是什么

### 2.1 一句话定义

> **MCP（Model Context Protocol）是 LLM 应用与外部上下文/工具之间的标准化通信协议。**
>
> 它解决了一个具体问题：以前每个 LLM 应用要单独对接每个工具，是 M × N 的对接成本；标准化后变成 M + N。

### 2.2 三方角色

```
+----------+        MCP         +-----------+        私有协议       +----------+
|   Host   | <----------------> |  Server   | <-------------------> |  系统    |
| Cursor / |   JSON-RPC 2.0     | (网关/    |   HTTP / gRPC /       |  GitHub  |
| Claude   |                    |  适配器)  |   DB / Filesystem...  |  Postgres|
+----------+                    +-----------+                       +----------+
```

| 角色 | 职责 | 例子 |
|------|------|------|
| **Host** | LLM 应用本体，负责对话、模型调用、UI | Cursor、Claude Desktop、Claude Code |
| **Server** | 暴露能力（工具、资源、提示模板） | 我们的 MCP Gateway、`@modelcontextprotocol/server-github` |
| **Backend** | Server 背后真实的系统 | 公司订单服务、Postgres、文件系统 |

注意：**Host 也是 MCP Client**。Host 进程内嵌一个 Client，Client 负责跟 Server 讲 MCP 协议。

### 2.3 生态全景（截至 2026 H1）

**Host（LLM 应用）已支持 MCP 的**：

- Cursor 0.45+
- Claude Desktop 0.8+
- Claude Code（Anthropic 官方 CLI）
- VS Code Copilot（agent mode）
- Windsurf、Cline、Continue
- OpenAI Codex（2025 Q4 起）
- Zed、Replit Agent、Sourcegraph Cody

**Server（生产可用的）**：

- 官方仓库 `modelcontextprotocol/servers` 已有 100+ 个：filesystem / postgres / github / gitlab / slack / google-drive / sentry / brave-search…
- 商业封装：Docker MCP Gateway（聚合上千个）、Composio、ContextHub
- **企业自建（本文重点）**：Swagger 转 MCP、内部 RPC 转 MCP

---

## 3. 协议深度拆解

MCP 不是 HTTP，不是 gRPC，它是建立在它们之上的应用层协议。下面按"运输 → 编码 → 握手 → 原语"四层讲。

### 3.1 编码层：JSON-RPC 2.0

所有消息都是 JSON-RPC 2.0：

```json
// Client → Server: 列出工具
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list"
}

// Server → Client: 返回
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      { "name": "list_services", "description": "...", "inputSchema": {...} },
      ...
    ]
  }
}

// Notification（无 id，不期待响应）：工具列表变更
{
  "jsonrpc": "2.0",
  "method": "notifications/tools/list_changed"
}
```

> **选 JSON-RPC 的妙处**：所有现代语言都能轻松解析，调试可读，又有标准化的 request/response/notification 三种语义。

### 3.2 传输层：双形态

MCP 规范里官方支持两种传输：

| 传输 | 适用 | 我们的端点 |
|------|------|------------|
| **stdio** | 进程间，Server 跟 Host 同机 | `mcpg-stdio-bridge` CLI |
| **Streamable HTTP**（2025-03-26 规范）| 远程 Server，一个 URL 双向通信 | `POST/GET /mcp` |
| **HTTP+SSE**（2024-11 旧版）| 旧版客户端 | `GET /mcp/sse` + `POST /mcp/message` |

**Streamable HTTP 是当前主流**。一个 URL：
- 客户端 → 服务端用 POST 发 JSON-RPC 请求
- 服务端 → 客户端用 GET 打开 SSE 长连接接收 notification

这里有一个**容易踩的坑**：Cursor 0.45 切到 Streamable HTTP 之后，老的"只支持 GET /sse"实现会直接 500。我们的 demo 同时支持新旧两套：

```12:25:mcpg-web/src/main/java/com/mcpg/web/mcp/McpController.java
@RestController
public class McpController {

    /** Modern Streamable HTTP. */
    @PostMapping("/mcp")
    public ResponseEntity<?> rpcOverHttp(...) { ... }

    @GetMapping(value = "/mcp", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter notificationsStream(...) { ... }

    /** Legacy HTTP+SSE. */
    @GetMapping(value = "/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter legacySseStream(...) { ... }

    @PostMapping("/mcp/message")
    public ResponseEntity<?> legacyInboundMessage(...) { ... }
}
```

### 3.3 握手：Capability Negotiation

Server 一上线先公告自己支持什么：

```json
// Client → Server: initialize
{
  "jsonrpc": "2.0",
  "id": 0,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-03-26",
    "capabilities": {
      "sampling": {},
      "roots": { "listChanged": true }
    },
    "clientInfo": { "name": "Cursor", "version": "0.45.2" }
  }
}

// Server → Client
{
  "jsonrpc": "2.0",
  "id": 0,
  "result": {
    "protocolVersion": "2025-03-26",
    "capabilities": {
      "tools": { "listChanged": true },
      "resources": { "subscribe": true, "listChanged": true },
      "prompts": { "listChanged": true },
      "logging": {}
    },
    "serverInfo": { "name": "mcpg", "version": "1.0.0" }
  }
}
```

**为什么要握手而不是大家都默认全功能？**
因为 MCP 设计目标之一是"递进式互操作"：一个简陋的 Server 只支持 tools 也是合法的，Host 看到 capabilities 之后只渲染对应 UI，不会因为对方没实现 prompts 就报错。

### 3.4 三大原语

MCP 协议主体是三种"原语"（Primitives），分别对应 LLM 工具链的三种典型动作：

| 原语 | 语义 | 典型用法 | Host 表现 |
|------|------|----------|-----------|
| **Tools** | 副作用动作（"做点啥"） | 调接口、写文件、执行 SQL | 模型自主调用 |
| **Resources** | 只读上下文（"看点啥"） | 文件、配置、文档 | 用户主动 `@` 引用 |
| **Prompts** | 模板化对话起手势 | "代码 review"、"生成单元测试" | 用户从下拉菜单选 |

> 在我们的 demo 里目前只用了 **Tools**。Resources 和 Prompts 是后续治理引擎要落的（比如把每个微服务的最新设计文档作为 Resource 暴露）。

#### 3.4.1 Tools 详解（重点）

一个 Tool 长这样：

```json
{
  "name": "order_service__getOrder",
  "description": "Get order by id. Returns full order with line items.",
  "inputSchema": {
    "type": "object",
    "properties": {
      "id": { "type": "string", "description": "Order id." }
    },
    "required": ["id"]
  },
  "annotations": {
    "destructiveHint": false,
    "readOnlyHint": true
  }
}
```

调用过程：

```
1. Client 拉取 tools/list
2. LLM 收到工具列表（连同 description + inputSchema 一起喂进 system prompt）
3. LLM 决定调用 → Client 发 tools/call
4. Server 执行 → 返回 result（structured content 或 text）
5. LLM 把 result 当作下一轮的输入
```

#### 3.4.2 Sampling（反向调用，进阶）

MCP 还有一个少被讨论但很关键的能力：**Server 可以反过来让 Host 帮自己调一次 LLM**。

```
Server → Client: sampling/createMessage
       { messages: [...], maxTokens: 1000 }
Client → 用 Host 的 LLM 跑一次 → 返回结果给 Server
```

**典型场景**：Server 在执行某个工具时，需要 LLM 帮自己理解一段自然语言指令（比如把"取消上周的所有订单"翻译成 SQL）。Server 不用自己买 API key，**借 Host 已有的 LLM 通道**。

> Sampling 是 MCP 跟传统 RPC 最不一样的设计 —— 通讯不是单向的。我们 demo 还没用，但治理引擎下一步要用它做"高风险操作的 LLM 二次确认"。

### 3.5 协议演进

| 版本 | 时间 | 关键变化 |
|------|------|----------|
| 2024-11 | Anthropic 发布初版 | HTTP+SSE 双端点 |
| 2025-03-26 | 当前主流 | Streamable HTTP（单端点）、structured tool output、annotations |
| 2025-06 | 最新 | OAuth 2.0 鉴权流程标准化、elicitation（Server 反向问 Host 用户） |

记住版本号是因为**Cursor、Claude Desktop 在升级期会有不兼容窗口**，落地企业 Gateway 时必须同时支持新旧。

---

## 4. "我手写一个 RPC 不行吗" —— 协议价值的硬核论证

这是被问得最多的一个问题。论证分三层。

### 4.1 表面看：好像确实可以

Cursor 0.45 之前我们也能让 AI 调内部接口 —— 写一个 Cursor extension，拦截输入，让 AI 调用我们自己的 HTTP endpoint。能跑。

### 4.2 但 MCP 解决了五个你手写 RPC 不会立刻意识到的问题

#### 问题 1：标准化的"工具暴露语义"

LLM 决定调用哪个工具，**不是看代码注释，是看 `inputSchema` + `description` + `annotations`**。MCP 把这三件套规范化了：

- `description` 给 LLM 决策用
- `inputSchema` 给 LLM 构造参数用（JSON Schema）
- `annotations.{readOnlyHint, destructiveHint, idempotentHint}` 给 Host UI 决定是否需要二次确认

手写 RPC 时这套元数据没有标准结构，每个项目各自发明一套，**LLM 的选择准确率会随项目数量线性下降**。

#### 问题 2：双向通讯 + 通知

MCP 是有状态长连接（SSE），Server 可以主动推送：

- `notifications/tools/list_changed` —— 工具列表变了，Host 立即重新拉取
- `notifications/resources/updated` —— 你订阅的资源（比如某个 README）变了
- `sampling/createMessage` —— Server 反向请求 LLM

手写 RPC 一般是无状态请求/响应，做不了"AI 工具列表运行时变化"这件事。我们 demo 里改 exposure mode 是热生效的，就是靠这个：

```5:35:mcpg-web/src/main/java/com/mcpg/web/mcp/ToolsChangedListener.java
@Component
public class ToolsChangedListener {
    @EventListener
    public void onToolsChanged(ToolsChangedEvent ev) {
        // Broadcast notifications/tools/list_changed to every SSE subscriber
        ...
    }
}
```

#### 问题 3：跨 Host 互操作（生态价值）

我手写一个对接 Cursor 的协议，Claude Desktop 用不了；再写一个对接 Claude 的，Codex 又用不了。

MCP 把这一层抽走 —— **一个 Server 同时被 6+ 个 Host 直接消费**。我们 demo 的同一个 `http://localhost:8088/mcp` URL，Cursor、Claude Desktop、Codex、Claude Code、Windsurf、Cline、Continue 都能直接接进去。这是协议的"网络效应"。

#### 问题 4：递进式 capability 协商

手写协议要么 all-or-nothing（不实现某接口直接 404），要么得自己定一套版本/能力声明。MCP 内置 capability negotiation，**老 Server 接新 Host 不会出错，新 Server 接老 Host 自动降级**。这对企业落地很关键 —— 我们不可能要求公司每个 BU 升级 Cursor 到同一版本。

#### 问题 5：安全模型

MCP 规范里明确划分了三类操作：

- **Tool**：必须经过 Host UI 的用户授权（点"允许"才会执行）
- **Resource**：只读，不需要授权
- **Sampling**：Server 反向请求 LLM，Host 可以拦截 / 重写 / 拒绝

手写 RPC 时这些安全语义要自己设计，而且每个 Host 实现不一样。MCP 把"模型不该不经用户同意就直接调用副作用接口"这条规则**烙在协议层**。

### 4.3 一句话总结

> **手写 RPC 解决"能调通"；MCP 解决"标准化、可发现、可治理、可生态"。**
>
> 这跟 1990s 各家发明自家 RPC vs. HTTP 一统天下、2010s 各家发明 SOAP/Thrift vs. gRPC 收敛是同一个故事，只是换到了 LLM 时代。

---

## 5. 企业现实：千服务 × 万接口 × LLM

回到我们公司语境。

### 5.1 矛盾点

| 现实 | 矛盾 |
|------|------|
| 公司有 N 个 BU、几百个微服务 | LLM 工具列表会爆炸 |
| 每个微服务都有 Swagger / OpenAPI | 但格式各异（v2、v3、Springdoc、Springfox、FastAPI） |
| 服务地址在 Nacos / Eureka / K8s 里 | LLM 不可能直接读注册中心 |
| 部分接口是写操作（删除、退款） | 不能让 LLM 随便点 |
| 不同环境（dev/staging/prod）权限不同 | LLM 不能跨环境串 |
| 接口在持续变化 | 工具列表要每天自动同步 |

### 5.2 直接给每个微服务装一个 MCP Server 行不行

理论可以。实际三个坎：

1. **侵入式**：要改每个服务的代码（加 MCP SDK、改启动）
2. **治理失控**：每个服务自治，没法统一审计、限流、写操作审批
3. **工具列表爆炸**：500 个服务 × 平均 20 个接口 = 1 万个 Tool 名字塞进 LLM 上下文 —— context budget 立刻爆

所以企业落地必然走 **Gateway 模式**：

```
    Cursor / Claude Code / Codex
            |
            | MCP (一个 URL)
            v
    +--------------------+
    |   MCP Gateway      | <-- 我们做的事
    +--------------------+
            |
            | Nacos / Eureka / Swagger
            v
    +--------------------+
    |  公司微服务集群    |
    +--------------------+
```

---

## 6. 解法：MCP Gateway Enterprise 架构与设计决策

> 完整代码在 [github.com/jialiuyang/MCP-Api-Gateway](https://github.com/jialiuyang/MCP-Api-Gateway)
> Demo 脚本：[`docs/demo-script.md`](./demo-script.md)
> 架构图：[`ARCHITECTURE.md`](../ARCHITECTURE.md) + [`docs/architecture.html`](./architecture.html)

### 6.1 模块布局（Maven 多模块）

```
mcpg-core/             SPI 接口、领域模型（无 Spring 依赖）
mcpg-parser/           OpenAPI v2 / v3 双解析器
mcpg-registry/         注册中心适配（Nacos / Eureka 实现 + 4 个 stub）
mcpg-server/           MCP server 实现：4 个元工具 + HTTP/SSE 端点
mcpg-web/              Spring Boot 主应用：REST、JPA、调度、治理
mcpg-stdio-bridge/     stdio 桥接 CLI（给只支持 stdio 的客户端）
mcpg-ui/               Vue 3 + Element Plus 控制台
```

技术栈：Java 17 / Spring Boot 3.3 / Spring Data JPA / H2 dev + MySQL prod / `io.swagger.parser.v3:swagger-parser` / Vue 3 / Element Plus / Pinia / vue-i18n / ECharts。

### 6.2 设计决策一：元工具模式（Meta-Tool Pattern）

这是整个项目最关键的一个决定。

**问题**：500 服务 × 20 接口 = 1 万个 tool 进 LLM 上下文，会怎样？

- 上下文 token 数飙升（gpt-4 是 128k context，光工具列表就吃掉一半）
- LLM 选工具准确率下降（"猜"的概率上升）
- 客户端 UI 列表无法浏览

**方案**：不要直接暴露每个接口为一个 tool。只暴露 **4 个元工具**：

| 元工具 | 语义 |
|--------|------|
| `list_services` | 列出网关感知的所有服务 |
| `search_api` | 按关键字在 tool 名、摘要、路径、tag 中搜索 |
| `get_api_schema` | 拿到某个接口完整的 JSON Schema（包含出参） |
| `call_api` | 真正调用某个接口 |

LLM 用法变成：

```
用户："取消订单 12345"
→ LLM 调 search_api("cancel order")          // 找候选
→ LLM 调 get_api_schema("order_service__cancelOrder")  // 拿 schema
→ LLM 调 call_api("order_service__cancelOrder", {id: 12345})  // 执行
→ 返回结果
```

**4 个 tool 永远固定**，不管后端有多少接口。LLM 上下文恒定 << 1k token，准确率反而上升（因为不需要从 1 万选 1）。

> 这跟 ChatGPT Custom Actions 早期遇到的同一个问题。OpenAI 现在也在朝 meta-tool 方向走，但 MCP 把这种自由度交给 server 端，更灵活。

### 6.3 设计决策二：三档暴露策略 + Promote 机制

只有元工具够吗？不够。两种场景需要补：

- **demo / 单服务**：直接把所有接口暴露给 LLM，让它一目了然
- **高频接口**：比如 `getOrder` 每天调几千次，每次都走 search 太啰嗦

所以我们做了三档 **ExposureMode**：

| 模式 | 暴露 | 适用 |
|------|------|------|
| `META` | 只暴露 4 个元工具 | 大规模生产，最严格 |
| `HYBRID`（默认） | 元工具 + 运营 promote 的高频接口 | 推荐 |
| `DIRECT_ALL` | 所有非废弃接口直接暴露 | demo / 单服务 |

**Promote** 是手动操作：UI 上点一下，某个 tool 从"藏在元工具背后"提升为"一级 MCP tool"。运营据此把 80% 流量收敛到 20% 接口上。

切换模式是热生效的 —— 改完后端立刻广播 `notifications/tools/list_changed`，Cursor 不用重启就刷新列表。

### 6.4 设计决策三：SPI 化的注册中心适配

公司可能用 Nacos，朋友公司可能用 Eureka，K8s 集群里又是 Service Discovery。我们用 SPI 抽象：

```java
public interface ServiceRegistryAdapter {
    String type();                                    // "nacos" / "eureka" / ...
    TestConnectionResult test(RegistryConfig cfg);
    List<DiscoveredService> discover(RegistryConfig cfg);
}
```

实现：Nacos、Eureka 真实实现；Consul / Polaris / K8s Service / Zookeeper 各做了一个**空壳类**（实现接口但 throw NotImplementedException）。意义：

- UI 上能立刻看到全部注册中心选项（产品路线图可见）
- 后续补全只需要替换 stub，不动其他模块代码

我们对 Nacos / Eureka 实现没有用 Spring Cloud Discovery —— 它带太多间接依赖（Ribbon / Hystrix）。**只用 nacos-client / 直接 HTTP + Basic auth 调 Eureka REST API**，启动快 30%、依赖纯净。

### 6.5 设计决策四：每日 Swagger 刷新

接口在变。每天凌晨 3:00 自动跑一次：

```java
@Scheduled(cron = "${mcpg.scheduler.swagger-refresh.cron:0 0 3 * * *}")
public void runDaily() { ... }
```

**关键设计**：刷新对每个服务串行执行 + 异常隔离。一个服务的 Swagger 文档坏了不影响其他。

而且刷新有"幂等"语义：
- 接口已存在 → 更新 schema，**保留 `promoted` 状态**（不能因为重新拉一次就把运营 promote 的接口反指）
- 接口新增 → 插入
- 接口下线 → 标记为 `deprecated`（不直接删，因为审计回溯需要）

### 6.6 设计决策五：把 SPA 打进单 Jar

Vue 前端 dist 通过 `frontend-maven-plugin` 编译，然后用 `maven-resources-plugin` 复制到 `mcpg-web/src/main/resources/static`。Spring Boot 启动后，控制台 UI 跟后端是同一个进程同一个端口。运维只需要 `java -jar mcpg-web.jar`。

---

## 7. 治理与生产化（B5）

仅仅"能跑通"在企业里远远不够。MCP Gateway Enterprise 的 B5 着重在治理层：

### 7.1 审计日志（Audit Log）

每一次：
- MCP 调用（`tools/call` 谁调的哪个 tool）
- 写操作（promote、配置修改、服务删除）
- 暴露策略变更

都进审计表，包含：actor、action、resource、outcome、HTTP status、duration、client IP、user-agent。

> demo 演示用了内存生成器（确定性 seed 64 条），生产环境会换成 JPA + 定期归档到对象存储。

### 7.2 治理策略卡（Policy Cards）

可在 UI 上开启/关闭/调整严重级别的 6 张卡：

| 策略 | 类别 | 默认 |
|------|------|------|
| Write operation guard | governance | 开启 |
| Global rate limit | traffic | 开启 |
| Environment isolation | governance | 开启 |
| Audit retention (90 days) | compliance | 开启 |
| PII redaction | compliance | 关闭 |
| SSO required | auth | 关闭 |

策略实体设计上特意把配置存成 JSON（`config_json`），新加策略无需 DDL 改动。

### 7.3 工具健康度（Health）

聚合每个 tool 的：
- 24h 调用量
- 成功率 / P50 / P95 / P99
- 最后调用时间、最后一条错误

Demo 用确定性 seed 生成（每次刷新数字稳定，方便录屏 / 截图）。生产接 Prometheus 或自有指标系统。

### 7.4 站点设置

- 默认环境（dev/staging/prod）
- Swagger 刷新 Cron
- 单服务最大工具数（防御异常 spec 把网关压垮）
- SSO 开关
- 审计保留天数

### 7.5 写操作分级（未来）

LLM 调写接口前，Gateway 拦截并按 **RiskLevel** 处理：

| 级别 | 触发条件 | 行为 |
|------|----------|------|
| `READ` | GET / HEAD | 直放 |
| `WRITE_LOW` | POST + 无敏感关键词 | 直放 |
| `WRITE_HIGH` | DELETE / refund / drop / payout | **二次 LLM 确认 + 人工审批** |
| `FORBIDDEN` | 显式黑名单 | 拒绝 |

启发式判定已经在 `ToolNaming.inferRiskLevel()` 实现，UI 也展示了 risk 列。审批流程是后续里程碑。

---

## 8. 现场 Demo（5 分钟）

> 完整脚本：[`docs/demo-script.md`](./demo-script.md)
> 简版流程图：[`docs/flow.html`](./flow.html)

### 顺序

1. **`mvn clean package` + `java -jar`** —— 启动后控制台已有 3 个种子服务（order / user / petstore）
2. **MCP Governance → Client Integration** —— 复制 Cursor 配置，粘贴 `~/.cursor/mcp.json`
3. **重启 Cursor**，看到 8 个 tool（4 元 + 4 promote）
4. **Cursor 聊天**：
   > "列出网关全部服务，搜 order 相关接口，把第一个的 schema 拿出来"

   会顺序触发 `list_services` → `search_api` → `get_api_schema`
5. **切暴露模式 META → 热生效**，Cursor 工具列表自动从 8 个变成 4 个
6. **巡演 4 个 B5 页面**：审计 / 策略 / 健康 / 设置

---

## 9. 风险与未来

### 9.1 现实风险

| 风险 | 缓解 |
|------|------|
| MCP 规范在持续演进（2024-11 → 2025-03-26 → 2025-06），可能不兼容 | Gateway 同时支持新旧传输；客户端版本号在审计里 |
| LLM 可能错调用 `call_api` 把生产数据删了 | 写操作分级 + 环境隔离 + 写操作审批（B6+） |
| Swagger 不准 / 缺字段 | 解析容错 + manual override（已实现 `PUT /api/services/{id}` 改 baseUrl） |
| 网关本身被 DDoS | 全局 rate limit（已落策略卡，引擎在做） |

### 9.2 协议层正在发生的事（值得跟）

- **A2A (Agent-to-Agent Protocol)** —— Google 在推，把 MCP 的"工具调用"扩展到"agent 之间互相调用"。MCP 解决 LLM ↔ tool，A2A 解决 agent ↔ agent
- **MCP-UI** —— Server 不只暴露数据，还能描述自己的 UI 控件；Host 渲染对应的小组件，给用户更直观的交互
- **OAuth 2.0 in MCP** —— 2025-06 spec 落地的认证流程，Cursor 直接弹 OAuth 让你登录公司 SSO
- **Sampling 商用化** —— 借 Host 的 LLM 通道，Server 不用自带模型 API key，企业部署成本骤降

### 9.3 我们 demo 的下一步（B6+）

- 真实审计存储 + 归档
- 写操作 LLM 二次确认 + 人工审批流
- OAuth 2.0 / SSO 接入
- A2A 协议尝试 —— 把 MCP Gateway 自己也作为一个 A2A agent 暴露给上层编排

---

## 10. 学习路径与资源

### 10.1 入门（4 小时）

| 资源 | 形态 | 投入 |
|------|------|------|
| [官方规范主页 modelcontextprotocol.io](https://modelcontextprotocol.io/) | 文档 | 30 min 通读 |
| [Anthropic MCP 发布博客（2024-11-25）](https://www.anthropic.com/news/model-context-protocol) | 文章 | 15 min |
| [Anthropic MCP 入门视频系列（YouTube 官方频道）](https://www.youtube.com/@anthropic-ai) | 视频 | 1 h |
| [官方 Server 仓库 modelcontextprotocol/servers](https://github.com/modelcontextprotocol/servers) | 代码 | 浏览 30 min |
| Cursor 配 `@modelcontextprotocol/server-filesystem` 跑通 | 实操 | 30 min |

### 10.2 深入（一周）

| 资源 | 形态 | 收获 |
|------|------|------|
| [MCP Specification（最新版）](https://spec.modelcontextprotocol.io/) | 规范 | 协议字段、错误码、生命周期 |
| [TypeScript SDK 源码 modelcontextprotocol/typescript-sdk](https://github.com/modelcontextprotocol/typescript-sdk) | 代码 | Client/Server 双向实现 |
| [Java SDK 源码 modelcontextprotocol/java-sdk](https://github.com/modelcontextprotocol/java-sdk) | 代码 | 我们 demo 借鉴的实现 |
| [Docker MCP Gateway](https://github.com/docker/mcp-gateway) | 代码 | 同类项目对比 |
| 自己写一个 Server（filesystem 之外）：连公司 Confluence | 实操 | 真实场景 |

### 10.3 工程化（持续）

| 资源 | 形态 |
|------|------|
| [Discord: modelcontextprotocol](https://modelcontextprotocol.io/community) | 社区 |
| [LinkedIn / Twitter 关注 David Soria Parra（MCP 创始人）](https://twitter.com/dsp_) | 第一手消息 |
| [Cursor 更新日志](https://cursor.com/changelog) | 客户端实现 |
| [Anthropic Engineering Blog](https://www.anthropic.com/engineering) | 协议演进 |
| 我们公司内部群 / 本项目 issues | 落地实战 |

### 10.4 一句话推荐路线

> **第 1 天**：通读 modelcontextprotocol.io 主页 + Anthropic 博客，Cursor 接 `filesystem` server 跑通
> **第 2 天**：用 TypeScript SDK 写一个最小 Server（暴露当前时间的 tool）
> **第 3 天**：读 MCP 完整规范（约 30 页），把握 capability + lifecycle
> **第 4-5 天**：读本仓库代码，重点 `mcpg-server` 和 `mcpg-web/mcp/` 包
> **第 6-7 天**：选一个公司内部场景（Jira / Confluence / 内部接口），动手写一个 server

---

## 附录 A：演讲时可能会被问的问题（预答）

**Q1：MCP 跟 Function Calling 是一回事吗？**
A：不是。Function Calling 是 LLM 厂商 API 的能力（GPT、Claude API 本身的参数），它定义"模型怎么告诉你它想调函数"。MCP 是上一层：定义"工具怎么对 LLM 应用暴露自己"。一个 MCP Server 可以同时被 GPT-4（Function Calling）、Claude（Tool Use）、Gemini（Function Calling）背后的 Host 应用消费。

**Q2：我们公司接口很多有鉴权（OAuth / 内部 token），怎么过？**
A：Gateway 持有真实凭证，Cursor 不接触。具体三种思路：
- 静态 token：网关配置注入 Authorization header
- OAuth 2.0：2025-06 MCP 规范支持，Cursor 弹窗让用户走 OAuth flow，token 由 Cursor 持有
- 服务账号：网关本身用一个低权限服务账号代调，结合环境隔离 + 写操作审批

**Q3：性能怎么样？万一 LLM 一秒打过来 100 次 call_api**
A：网关本身是 Spring Boot，单实例 1k+ QPS 没问题；瓶颈在下游服务。所以策略卡有 `rate-limit.global`，按 client + 全局双层限流。Cursor 本身也有客户端节流。

**Q4：跟 Docker MCP Gateway 比有什么差异？**
A：Docker 那个偏"聚合多个开源 MCP Server"（filesystem / postgres / github），是消费侧的工具集。我们这个是"把企业自有微服务变成 MCP"，目标完全不同，可以串起来用 —— Docker MCP Gateway 给 LLM 提供 GitHub、Slack，我们 Gateway 提供公司订单/用户/库存。

**Q5：LLM 错调了 call_api 把生产删了怎么办**
A：见 §7.5 写操作分级。三道闸：
1. 客户端 Host UI 默认每个 tool 第一次执行都要用户点"允许"
2. Gateway 侧 RiskLevel 启发式判定，WRITE_HIGH 走二次审批流
3. 环境隔离策略卡限制 prod 只能 allow-listed 用户访问
此外 prod 数据库默认连只读副本，物理上断掉写

**Q6：spec 在变，半年后我们的 Gateway 还能用吗？**
A：协议核心（JSON-RPC + 三大原语）非常稳定，变化集中在传输层和扩展能力。我们的 `McpController` 同时支持两个传输版本就是为这件事买保险。后续新增 capability 是叠加式的，不破坏老的。

---

## 附录 B：分享时的演示节奏建议

```
0-5min   Slide 1-5：起点 + 真实痛点 + MCP 定义
5-15min  Slide 6-12：协议深度（JSON-RPC、Capability、Tools、Sampling、双传输）
15-20min Slide 13-15："手写 RPC 不行吗" 五论
20-25min Slide 16-18：企业现实矛盾
25-35min Slide 19-25：架构 + 5 个设计决策
35-40min 现场 Demo（5min 脚本）
40-50min 治理 / 风险 / 未来
50-55min Q&A
```

---

## 附录 C：可复用的金句（演讲版）

- "MCP 不是某家公司的产品，是给 LLM 用的 USB 接口标准。"
- "手写 RPC 解决能调通，MCP 解决标准化、可发现、可治理、可生态。"
- "1 万个 tool 进 LLM 上下文 = context budget 立刻爆。所以我们用元工具，让 LLM 用 search 而不是 list。"
- "好的协议是让简陋的 Server 接老的 Host 也能跑，让新的 Server 接复杂的 Host 也能跑。MCP 做到了。"
- "MCP Gateway 不是让 AI 更智能，是让公司业务系统第一次被 AI 真正『看见』。"

---

**完。**
**仓库**：<https://github.com/jialiuyang/MCP-Api-Gateway>
**反馈/交流**：本项目 Issues
