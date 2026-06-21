/**
 * Simplified Chinese strings. This bundle is the source of truth - new keys
 * land here first, then get mirrored in en-US.ts.
 */
export default {
  app: {
    name: 'MCP 网关',
    fullName: 'MCP 网关企业版'
  },
  language: {
    label: '语言',
    zh: '简体中文',
    en: 'English'
  },
  menu: {
    dashboard: '仪表盘',
    registries: '服务注册中心',
    services: '服务',
    mcp: 'MCP 治理',
    tools: '工具',
    clients: '客户端接入',
    audit: '审计日志',
    policies: '策略',
    health: '工具健康度',
    settings: '设置'
  },
  layout: {
    expandSidebar: '展开侧栏',
    collapseSidebar: '折叠侧栏',
    live: '在线'
  },
  common: {
    search: '搜索',
    save: '保存',
    cancel: '取消',
    delete: '删除',
    edit: '编辑',
    refresh: '刷新',
    confirm: '确认',
    create: '新建',
    actions: '操作',
    all: '全部',
    none: '无',
    loading: '加载中…',
    yes: '是',
    no: '否',
    copyToClipboard: '复制到剪贴板',
    copied: '已复制到剪贴板',
    clipboardError: '无法访问剪贴板，请手动复制',
    notImplemented: '尚未实现'
  },
  dashboard: {
    cards: {
      registries: '注册中心',
      services: '服务',
      tools: '工具',
      calls24h: '近 24 小时调用'
    },
    cardSub: {
      registriesPending: 'B3 — 注册中心适配',
      registriesEmpty: '尚未配置注册中心',
      registriesActive: '{enabled} 已启用',
      servicesEmpty: '导入第一个 OpenAPI 文档',
      servicesActive: '运行中',
      toolsPromoted: '{count} 已提升',
      callsPending: '尚无指标数据',
      callsLive: '近 24h · 成功率 {rate}%'
    },
    welcomeTitle: '欢迎使用 MCP 网关',
    welcomeBody:
      '在 {servicesLink} 页面手动导入 OpenAPI 3.x 或 Swagger 2.0 文档 URL，工具会在几秒内出现在 {toolsLink} 页面。' +
      '按照 {cursorLink} 页面的说明把 MCP 客户端指向本网关即可使用。',
    appInfo: {
      application: '应用',
      version: '版本',
      timestamp: '服务时间',
      status: '状态'
    },
    roadmap: '路线图',
    roadmapItems: {
      b1: 'B1 — 工程骨架与构建链路',
      b2: 'B2 — OpenAPI 解析、元工具、Cursor 接入',
      b3: 'B3 — 注册中心适配 + 每日刷新',
      b4: 'B4 — 暴露策略 + Promote 机制',
      b5: 'B5 — 高保真 UI + Demo + 完整文档'
    }
  },
  services: {
    title: '服务',
    subtitle:
      '所有被网关感知的服务——可在本页面手动导入 OpenAPI 3.x 或 Swagger 2.0 文档，也可由注册中心适配器自动发现（B3）。',
    importButton: '导入',
    columns: {
      name: '名称',
      env: '环境',
      source: '来源',
      baseUrl: '基础地址',
      tools: '工具数',
      status: '状态',
      lastSynced: '最近同步'
    },
    filters: {
      keywordPlaceholder: '名称或显示名',
      environmentAll: '全部'
    },
    status: {
      ACTIVE: '正常',
      ERROR: '异常',
      DISABLED: '已停用'
    },
    actions: {
      edit: '编辑',
      refresh: '刷新',
      delete: '删除'
    },
    empty: {
      description: '还没有任何服务',
      cta: '导入 OpenAPI / Swagger'
    },
    deleteConfirm: {
      title: '确认删除',
      message: '删除服务"{name}"和其下所有工具？该操作不可撤销。',
      confirmText: '删除',
      cancelText: '取消'
    },
    messages: {
      imported:
        '已导入"{name}"，共 {count} 个工具（新增 {added} / 更新 {updated} / 删除 {removed}）',
      refreshed: '已刷新"{name}"：新增 {added} / 更新 {updated} / 删除 {removed}',
      deleted: '服务"{name}"已删除',
      updated: '服务已更新',
      nameUrlRequired: '名称和文档 URL 为必填',
      baseUrlInvalid: '基础地址必须以 http:// 或 https:// 开头'
    },
    importDialog: {
      title: '导入 OpenAPI / Swagger 文档',
      subtitle:
        '支持 OpenAPI 3.0 / 3.1 与 Swagger 2.0；YAML、JSON 皆可。文档内容会由解析器嗅探后自动选用对应解析器。',
      formatsLabel: '已支持格式',
      formats: 'OpenAPI 3.x（JSON / YAML） · Swagger 2.0（JSON / YAML）',
      commonPathsLabel: '常见路径',
      commonPaths: '/openapi.json · /openapi.yaml · /v3/api-docs · /v3/api-docs.yaml · /v2/api-docs · /swagger.json',
      fields: {
        name: '名称',
        displayName: '显示名',
        url: '文档 URL',
        baseUrl: '基础地址',
        environment: '环境'
      },
      placeholders: {
        name: 'order-service',
        displayName: 'Order Service（生产环境）',
        url: 'https://api.example.com/openapi.json 或 /v3/api-docs / /v2/api-docs',
        baseUrl: '（可选）覆盖 spec 中的 servers[0]'
      },
      submit: '导入'
    },
    editDialog: {
      title: '编辑服务',
      fields: {
        displayName: '显示名',
        baseUrl: '基础地址',
        environment: '环境'
      },
      baseUrlHint: '必须是完整 URL，以 {http} 或 {https} 开头。网关使用此地址调用后端服务。',
      submit: '保存'
    }
  },
  tools: {
    title: '工具',
    subtitle:
      '从已导入的 OpenAPI / Swagger 文档解析出的接口。四个元工具（{listServices}、{searchApi}、{getApiSchema}、{callApi}）始终对 MCP 客户端可见；在此提升（Promote）的接口会以一级工具的形式额外暴露。',
    columns: {
      toolName: '工具名',
      service: '所属服务',
      method: '方法',
      path: '路径',
      summary: '摘要',
      risk: '风险',
      promoted: '已提升'
    },
    filters: {
      keywordPlaceholder: '名称、摘要、路径',
      servicePlaceholder: '全部服务'
    },
    risk: {
      READ: '只读',
      WRITE_LOW: '写（低风险）',
      WRITE_HIGH: '写（高风险）',
      FORBIDDEN: '禁止'
    },
    promote: {
      promoted: '已将"{name}"提升为一级 MCP 工具',
      demoted: '已将"{name}"降回元工具背后'
    },
    empty: {
      description: '没有匹配的工具',
      cta: '前往服务页面导入 OpenAPI 文档'
    },
    drawer: {
      title: '工具详情',
      labels: {
        path: '路径',
        service: '服务',
        operationId: '操作 ID',
        summary: '摘要',
        tags: '标签'
      },
      sections: {
        description: '描述',
        inputSchema: '入参 Schema',
        outputSchema: '出参 Schema'
      },
      deprecated: '已废弃'
    }
  },
  cursor: {
    title: '客户端接入',
    subtitle:
      '把任何 MCP 客户端（Cursor、Claude Desktop、Cline、Continue、Windsurf…）指向本网关，LLM 就能获得四个元工具（{listServices}、{searchApi}、{getApiSchema}、{callApi}）以及任何被你提升的接口。',
    endpoints: {
      httpLabel: 'Streamable HTTP 端点（推荐）',
      sseLabel: 'HTTP+SSE 旧版端点',
      copyUrl: '复制 URL'
    },
    tabs: {
      http: 'Streamable HTTP（推荐）',
      sse: 'HTTP+SSE（旧版）',
      stdio: 'stdio 桥接'
    },
    http: {
      intro:
        '现行 MCP HTTP 传输协议（spec 2025-03-26）。Cursor 0.45+、Claude Desktop ≥ 0.8、Cline、Continue、Windsurf 都已原生支持。一个 URL 同时处理客户端→服务端请求（POST）和服务端→客户端通知（GET / SSE）。',
      step1Title: '1. 编辑你的 MCP 客户端配置文件',
      step1Body: '下面是常见客户端的配置位置；文件不存在则按下面的内容新建。',
      step1Files: {
        cursor: 'Cursor：{path}（Windows：{winPath}）',
        claude: 'Claude Desktop：{path}（Windows：{winPath}）',
        cline: 'Cline（VS Code 插件）：{path}',
        windsurf: 'Windsurf：{path}'
      },
      copyConfig: '复制配置',
      step2Title: '2. 重启客户端',
      step2Body:
        '重启后打开客户端的 Tools / MCP 面板：你应该能看到 4 个以 {listServices}、{searchApi}、{getApiSchema}、{callApi} 开头的工具。被提升的接口也会一起显示。',
      step3Title: '3. 试用一下',
      step3Body: '可以让助手做这样的事：',
      step3Example:
        '列出网关上所有服务，然后搜 "order" 相关的接口，把第一个的 schema 拿出来给我看。'
    },
    sse: {
      hint: '仅在你的 MCP 客户端早于 Streamable HTTP 协议时使用。',
      configTitle: '配置',
      copyConfig: '复制配置',
      explainer:
        '配置中的 {typeFlag} 会强制旧版 Cursor 使用 2024-11 传输协议（先 GET {sse}，再 POST 到带 sessionId 的 URL）。'
    },
    stdio: {
      intro:
        'stdio 桥接是一个细长 CLI，把 stdin/stdout 代理到网关的 SSE 通道。当 MCP 客户端只能讲 stdio，或者网关在防火墙后无法直连 SSE 时使用。',
      step1Title: '1. 构建桥接',
      step2Title: '2. 编辑 MCP 配置',
      copyConfig: '复制配置',
      hint1: '把 {placeholder} 换成实际构建出的 jar 路径（默认在 {defaultPath}）。',
      hint2: '桥接内部其实走的是 Streamable HTTP，{flag} 指向网关的 {endpoint} 端点。'
    },
    metaTools: {
      title: '元工具如何工作',
      steps: {
        listServices: '枚举网关已知的全部服务。可选环境过滤。',
        searchApi: '按关键字在工具名、摘要、路径、标签中搜索。返回精简列表。',
        getApiSchema: '返回某个接口完整的 JSON Schema（含出参 schema），LLM 据此构造参数。',
        callApi: '真正调用后端接口。响应原样返回，让 LLM 自行推理。'
      }
    }
  },
  registries: {
    title: '服务注册中心',
    subtitle:
      '配置 Nacos / Eureka 等注册中心。开启后，网关每 5 分钟轮询一次，自动把新出现的服务（及其 OpenAPI / Swagger 文档）转成 MCP 工具。',
    addButton: '新增注册中心',
    columns: {
      name: '名称',
      type: '类型',
      endpoint: '地址',
      env: '环境',
      services: '服务数',
      status: '状态',
      lastSynced: '最近同步',
      enabled: '启用'
    },
    status: {
      UNKNOWN: '未知',
      OK: '正常',
      ERROR: '异常'
    },
    actions: {
      test: '测试',
      discover: '立即发现',
      edit: '编辑',
      delete: '删除'
    },
    empty: {
      description: '还没有配置任何注册中心',
      cta: '新增注册中心'
    },
    deleteConfirm: {
      title: '确认删除',
      message: '删除注册中心"{name}"？历史上由它发现的服务保留不变。',
      confirmText: '删除',
      cancelText: '取消'
    },
    messages: {
      created: '注册中心"{name}"已创建',
      updated: '注册中心已更新',
      deleted: '注册中心"{name}"已删除',
      testOk: '连接成功（耗时 {ms} ms）',
      testFail: '连接失败：{reason}',
      discoveryOk: '"{name}" 发现 {discovered} 个服务，导入 {imported}，跳过 {skipped}',
      nameRequired: '名称必填',
      endpointRequired: '地址必填',
      stubNotImpl: '此类型为占位（后续实现），暂不能创建'
    },
    typeBadge: {
      preview: '即将上线'
    },
    dialog: {
      createTitle: '新增注册中心',
      editTitle: '编辑注册中心',
      fields: {
        name: '名称',
        displayName: '显示名',
        type: '类型',
        endpoint: '地址',
        username: '用户名',
        password: '密码',
        environment: '默认环境',
        namespace: '命名空间',
        groupName: '分组',
        extra: '额外参数',
        enabled: '启用'
      },
      placeholders: {
        name: 'nacos-prod',
        displayName: 'Nacos 生产集群',
        endpoint: '127.0.0.1:8848 或 http://eureka:8761',
        username: '可选',
        password: '可选；留空保持原密码',
        namespace: 'Nacos namespace id；Eureka 留空',
        groupName: 'Nacos 分组，默认 DEFAULT_GROUP',
        extra: 'k1=v1;k2=v2 形式，例如 preferIpAddress=true'
      },
      hints: {
        envHint: '所有由该注册中心发现的服务都会带上此环境标签',
        passwordHint: '编辑时留空表示保留原密码；填空字符串表示清除'
      },
      submit: '保存'
    },
    discoveryDialog: {
      title: '发现结果：{name}',
      summary: '共 {discovered}，导入 {imported}，跳过 {skipped}',
      columns: {
        name: '服务名',
        baseUrl: '探测地址',
        status: '状态',
        message: '说明'
      },
      itemStatus: {
        IMPORTED: '已导入',
        UPDATED: '已更新',
        SKIPPED: '已跳过'
      },
      close: '关闭'
    }
  },

  exposure: {
    currentLabel: '当前暴露策略',
    changeButton: '切换策略',
    dialogTitle: '修改暴露策略',
    saveButton: '保存',
    recommend: '推荐',
    noteLabel: '说明',
    notePlaceholder: '可选：记录这次变更的原因，方便后续审计回溯',
    confirmTitle: '确认切换到 DIRECT_ALL？',
    confirmDirectAll:
      'DIRECT_ALL 模式会把全部 {count} 个工具直接暴露给 LLM。工具数过多时 LLM 上下文预算会被快速吃光，仅建议 demo 或单服务场景使用。确定要切换吗？',
    confirmButton: '确定切换',
    updated: '已切换为 {mode} 模式',
    stats: {
      effective: '实际暴露',
      total: '工具总数',
      promoted: '已提升',
      meta: '元工具'
    },
    modes: {
      META: {
        label: 'META（仅元工具）',
        summary: '只暴露 4 个元工具，LLM 通过 search_api → call_api 间接调用所有接口。',
        desc: '只暴露 4 个元工具，所有真实接口都藏在元工具背后。'
      },
      HYBRID: {
        label: 'HYBRID（推荐）',
        summary: '元工具 + 运营手动提升（Promote）的高频接口。兼顾发现性和上下文预算。',
        desc: '元工具 + 运营提升的接口都直接暴露。'
      },
      DIRECT_ALL: {
        label: 'DIRECT_ALL（全暴露）',
        summary: '所有非废弃接口都作为一级工具直接暴露，跳过元工具。',
        desc: '把所有接口直接暴露给 LLM，不走元工具。',
        warn:
          '⚠️ 工具数 > 30 时 LLM 上下文预算容易爆炸，仅 demo / 单服务推荐。生产建议 HYBRID。'
      }
    }
  },

  placeholder: {
    pending: '功能将在 {milestone} 中实现',
    description: '此页面是为后续里程碑预留的入口，便于产品呈现完整路线图。'
  },

  audit: {
    title: '审计日志',
    subtitle:
      '记录所有写操作与 MCP 调用，便于追溯。当前演示数据由内存生成器提供，未来里程碑会落到持久化与归档。',
    filters: {
      keywordPlaceholder: '动作、用户、资源',
      outcomeAll: '全部结果',
      outcomeSuccess: '成功',
      outcomeFailure: '失败'
    },
    columns: {
      timestamp: '时间',
      actor: '发起者',
      action: '动作',
      resource: '资源',
      outcome: '结果',
      status: 'HTTP',
      duration: '耗时',
      detail: '详情'
    },
    outcome: {
      SUCCESS: '成功',
      FAILURE: '失败'
    },
    drawer: {
      title: '事件详情',
      sections: {
        basic: '基本信息',
        client: '客户端',
        detail: '详情'
      },
      labels: {
        id: 'ID',
        timestamp: '时间',
        actor: '发起者',
        action: '动作',
        resourceType: '资源类型',
        resourceId: '资源 ID',
        outcome: '结果',
        status: 'HTTP 状态',
        duration: '耗时 (ms)',
        clientIp: '客户端 IP',
        userAgent: 'User-Agent'
      }
    },
    empty: '没有匹配的审计事件'
  },

  policies: {
    title: '治理策略',
    subtitle:
      '高保真策略卡，可开启/关闭、调整严重级别、修改配置。后端 CRUD 已就绪；评估钩子将在治理引擎落地后接入。',
    filters: {
      keywordPlaceholder: '搜索策略',
      categoryAll: '全部分组',
      statusAll: '全部状态',
      statusEnabled: '已启用',
      statusDisabled: '已停用'
    },
    severity: {
      LOW: '低',
      MEDIUM: '中',
      HIGH: '高',
      CRITICAL: '关键'
    },
    category: {
      governance: '治理',
      traffic: '流量',
      compliance: '合规',
      auth: '认证'
    },
    actions: {
      enable: '启用',
      disable: '停用',
      edit: '编辑配置'
    },
    editDialog: {
      title: '编辑策略：{name}',
      fields: {
        enabled: '启用',
        severity: '严重级别',
        configJson: '配置（JSON）',
        note: '变更说明（可选）'
      },
      configHint: '保存前会校验是否为合法 JSON',
      jsonInvalid: '配置不是合法 JSON',
      submit: '保存',
      cancel: '取消'
    },
    messages: {
      toggled: '"{name}" {status}',
      updated: '"{name}" 配置已更新'
    },
    empty: '没有匹配的策略'
  },

  health: {
    title: '工具健康度',
    subtitle:
      '聚合每个 MCP 工具的成功率、调用量与延迟分布。当前数据由确定性生成器产生（demo 演示用），未来会接入真实指标系统。',
    kpis: {
      totalTools: '工具总数',
      activeTools: '活跃工具',
      calls24h: '近 24h 调用',
      successRate: '成功率',
      avgLatency: '平均延迟'
    },
    charts: {
      callVolumeTitle: '24 小时调用量',
      callVolumeSuccess: '成功',
      callVolumeFailure: '失败',
      latencyTitle: '延迟分布（P50）',
      latencyAxis: '调用次数'
    },
    table: {
      title: 'Top 工具',
      columns: {
        toolName: '工具',
        service: '服务',
        calls24h: '24h 调用',
        successRate: '成功率',
        p50: 'P50',
        p95: 'P95',
        p99: 'P99',
        lastInvoked: '最近调用',
        lastError: '最近错误'
      }
    },
    empty: '尚无工具，先去服务页面导入一份 OpenAPI 文档。'
  },

  settings: {
    title: '设置',
    subtitle: '站点级偏好，所有节点共享。',
    sections: {
      general: '通用',
      schedule: '调度',
      governance: '治理',
      demo: 'Demo'
    },
    fields: {
      siteName: '站点名称',
      defaultEnvironment: '默认环境',
      refreshCron: 'Swagger 刷新 Cron',
      maxToolsPerService: '单服务最大工具数',
      ssoEnabled: '启用 SSO',
      auditRetentionDays: '审计保留天数',
      demoMode: '保留演示数据',
      updatedAt: '最近更新'
    },
    hints: {
      cron: '六字段 Spring Cron，例如 0 0 3 * * * 表示每天凌晨 3 点',
      maxTools: '导入时超过该上限的接口会被截断，防止异常文档把网关压垮',
      sso: 'OSS 演示默认关闭；接入企业 SSO 后启用',
      demo: '关闭后下次启动不再自动注入示例服务'
    },
    submit: '保存',
    saved: '设置已保存'
  },

  errors: {
    networkError: '网络错误',
    notFound: '资源不存在'
  }
};
