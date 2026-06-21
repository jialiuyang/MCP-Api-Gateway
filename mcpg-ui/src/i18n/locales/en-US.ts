/**
 * English locale. Mirrors the key tree in zh-CN.ts; keep the two files
 * structurally identical so missing-key fallbacks behave predictably.
 */
export default {
  app: {
    name: 'MCP Gateway',
    fullName: 'MCP Gateway Enterprise'
  },
  language: {
    label: 'Language',
    zh: '简体中文',
    en: 'English'
  },
  menu: {
    dashboard: 'Dashboard',
    registries: 'Service Registry',
    services: 'Services',
    mcp: 'MCP Governance',
    tools: 'Tools',
    clients: 'Client Integration',
    audit: 'Audit Log',
    policies: 'Policies',
    health: 'Tool Health',
    settings: 'Settings'
  },
  layout: {
    expandSidebar: 'Expand sidebar',
    collapseSidebar: 'Collapse sidebar',
    live: 'Live'
  },
  common: {
    search: 'Search',
    save: 'Save',
    cancel: 'Cancel',
    delete: 'Delete',
    edit: 'Edit',
    refresh: 'Refresh',
    confirm: 'Confirm',
    create: 'Create',
    actions: 'Actions',
    all: 'All',
    none: 'None',
    loading: 'Loading…',
    yes: 'Yes',
    no: 'No',
    copyToClipboard: 'Copy to clipboard',
    copied: 'Copied to clipboard',
    clipboardError: 'Clipboard not available; please copy manually',
    notImplemented: 'Not implemented yet'
  },
  dashboard: {
    cards: {
      registries: 'Registries',
      services: 'Services',
      tools: 'Tools',
      calls24h: 'Calls (24h)'
    },
    cardSub: {
      registriesPending: 'B3 — stubbed adapters',
      registriesEmpty: 'No registries configured',
      registriesActive: '{enabled} enabled',
      servicesEmpty: 'Import your first OpenAPI doc',
      servicesActive: 'live',
      toolsPromoted: '{count} promoted',
      callsPending: 'No metrics yet',
      callsLive: 'last 24h · {rate}% success'
    },
    welcomeTitle: 'Welcome to MCP Gateway Enterprise',
    welcomeBody:
      'Manually import an OpenAPI 3.x or Swagger 2.0 document URL on the {servicesLink} page; ' +
      'tools appear on {toolsLink} within seconds. Point any MCP client at the gateway with the ' +
      'recipe on {cursorLink}.',
    appInfo: {
      application: 'Application',
      version: 'Version',
      timestamp: 'Server timestamp',
      status: 'Status'
    },
    roadmap: 'Roadmap',
    roadmapItems: {
      b1: 'B1 — Project skeleton + build pipeline',
      b2: 'B2 — OpenAPI parser + meta tools + Cursor',
      b3: 'B3 — Registry adapters + daily refresh',
      b4: 'B4 — Exposure strategies + Promote',
      b5: 'B5 — High-fidelity UI + demo + docs'
    }
  },
  services: {
    title: 'Services',
    subtitle:
      'Every service the gateway has learned about — manually import an OpenAPI 3.x or ' +
      'Swagger 2.0 document on this page, or let a registry adapter (B3) discover them.',
    importButton: 'Import',
    columns: {
      name: 'Name',
      env: 'Env',
      source: 'Source',
      baseUrl: 'Base URL',
      tools: 'Tools',
      status: 'Status',
      lastSynced: 'Last synced'
    },
    filters: {
      keywordPlaceholder: 'name or display name',
      environmentAll: 'All'
    },
    status: {
      ACTIVE: 'ACTIVE',
      ERROR: 'ERROR',
      DISABLED: 'DISABLED'
    },
    actions: {
      edit: 'Edit',
      refresh: 'Refresh',
      delete: 'Delete'
    },
    empty: {
      description: 'No services yet',
      cta: 'Import OpenAPI / Swagger'
    },
    deleteConfirm: {
      title: 'Confirm deletion',
      message: 'Delete service "{name}" and all its tools? This cannot be undone.',
      confirmText: 'Delete',
      cancelText: 'Cancel'
    },
    messages: {
      imported:
        'Imported "{name}" with {count} tools (+{added} / {updated} updated / -{removed})',
      refreshed: 'Re-pulled "{name}": +{added} / {updated} updated / -{removed}',
      deleted: 'Service "{name}" deleted',
      updated: 'Service updated',
      nameUrlRequired: 'Name and document URL are required',
      baseUrlInvalid: 'Base URL must start with http:// or https://'
    },
    importDialog: {
      title: 'Import an OpenAPI / Swagger document',
      subtitle:
        'OpenAPI 3.0 / 3.1 and Swagger 2.0 are both supported; YAML or JSON, the dialect is ' +
        'sniffed from the document content and dispatched to the matching parser automatically.',
      formatsLabel: 'Supported formats',
      formats: 'OpenAPI 3.x (JSON / YAML) · Swagger 2.0 (JSON / YAML)',
      commonPathsLabel: 'Common paths',
      commonPaths:
        '/openapi.json · /openapi.yaml · /v3/api-docs · /v3/api-docs.yaml · /v2/api-docs · /swagger.json',
      fields: {
        name: 'Name',
        displayName: 'Display name',
        url: 'Document URL',
        baseUrl: 'Base URL',
        environment: 'Environment'
      },
      placeholders: {
        name: 'order-service',
        displayName: 'Order Service (Prod-like)',
        url: 'https://api.example.com/openapi.json or /v3/api-docs / /v2/api-docs',
        baseUrl: '(optional) overrides servers[0] from the spec'
      },
      submit: 'Import'
    },
    editDialog: {
      title: 'Edit service',
      fields: {
        displayName: 'Display name',
        baseUrl: 'Base URL',
        environment: 'Environment'
      },
      baseUrlHint:
        'Must be a complete URL starting with {http} or {https}. This is the host the gateway uses when invoking tools.',
      submit: 'Save'
    }
  },
  tools: {
    title: 'Tools',
    subtitle:
      'Operations parsed from imported OpenAPI / Swagger documents. The four meta tools ' +
      '({listServices}, {searchApi}, {getApiSchema}, {callApi}) are always exposed to MCP ' +
      'clients; rows you promote here also become first-class tools.',
    columns: {
      toolName: 'Tool name',
      service: 'Service',
      method: 'Method',
      path: 'Path',
      summary: 'Summary',
      risk: 'Risk',
      promoted: 'Promoted'
    },
    filters: {
      keywordPlaceholder: 'name, summary, path',
      servicePlaceholder: 'All services'
    },
    risk: {
      READ: 'READ',
      WRITE_LOW: 'WRITE_LOW',
      WRITE_HIGH: 'WRITE_HIGH',
      FORBIDDEN: 'FORBIDDEN'
    },
    promote: {
      promoted: 'Promoted "{name}" to a direct MCP tool',
      demoted: 'Demoted "{name}" back behind the meta tools'
    },
    empty: {
      description: 'No tools match the filter',
      cta: 'Go to Services to import an OpenAPI document'
    },
    drawer: {
      title: 'Tool detail',
      labels: {
        path: 'Path',
        service: 'Service',
        operationId: 'Operation ID',
        summary: 'Summary',
        tags: 'Tags'
      },
      sections: {
        description: 'Description',
        inputSchema: 'Input schema',
        outputSchema: 'Output schema'
      },
      deprecated: 'Deprecated'
    }
  },
  cursor: {
    title: 'Client Integration',
    subtitle:
      'Point any MCP client (Cursor, Claude Desktop, Cline, Continue, Windsurf…) at this gateway ' +
      'and the LLM gains four meta tools ({listServices}, {searchApi}, {getApiSchema}, ' +
      '{callApi}) plus any operations you have promoted.',
    endpoints: {
      httpLabel: 'Streamable HTTP endpoint (recommended)',
      sseLabel: 'Legacy SSE endpoint',
      copyUrl: 'Copy URL'
    },
    tabs: {
      http: 'Streamable HTTP (recommended)',
      sse: 'HTTP+SSE (legacy)',
      stdio: 'stdio bridge'
    },
    http: {
      intro:
        'The current MCP HTTP transport (spec 2025-03-26). Cursor 0.45+, Claude Desktop ≥ 0.8, ' +
        'Cline, Continue and Windsurf all speak it natively. One URL handles both client → server ' +
        'requests (POST) and server → client notifications (GET / SSE).',
      step1Title: '1. Edit your MCP client config',
      step1Body: 'Common locations for the major clients; create the file if it does not exist.',
      step1Files: {
        cursor: 'Cursor: {path} (Windows: {winPath})',
        claude: 'Claude Desktop: {path} (Windows: {winPath})',
        cline: 'Cline (VS Code extension): {path}',
        windsurf: 'Windsurf: {path}'
      },
      copyConfig: 'Copy config',
      step2Title: '2. Restart the client',
      step2Body:
        'After restart, open the client\'s Tools / MCP panel: you should see four tools whose names ' +
        'start with {listServices}, {searchApi}, {getApiSchema} and {callApi}. Promoted operations ' +
        'appear alongside them.',
      step3Title: '3. Try it',
      step3Body: 'Ask the assistant something like:',
      step3Example:
        'List all services on the gateway, then search for "order" APIs and show me the schema of the first one.'
    },
    sse: {
      hint: 'Use only if your MCP client predates the Streamable HTTP transport.',
      configTitle: 'Config',
      copyConfig: 'Copy config',
      explainer:
        'The {typeFlag} hint forces older Cursor builds onto the legacy 2024-11 transport ' +
        '(GET {sse} first, then POST to a per-session URL).'
    },
    stdio: {
      intro:
        'The stdio bridge is a thin CLI that proxies stdin/stdout to the gateway\'s SSE channel. ' +
        'Use this transport when the MCP client only speaks stdio, or when the gateway is behind ' +
        'a corporate firewall that blocks SSE.',
      step1Title: '1. Build the bridge',
      step2Title: '2. Edit your MCP config',
      copyConfig: 'Copy config',
      hint1: 'Replace {placeholder} with the location of the built jar (defaults to {defaultPath}).',
      hint2: 'The bridge itself talks Streamable HTTP under the hood; the {flag} flag points at the gateway\'s {endpoint} endpoint.'
    },
    metaTools: {
      title: 'How the meta tools work',
      steps: {
        listServices: 'Enumerates every service known to the gateway. Optional environment filter.',
        searchApi:
          'Finds tools whose name, summary, path or tags match the keyword. Returns a compact Markdown list.',
        getApiSchema:
          'Returns the full JSON Schema (and output schema if available) so the LLM knows how to shape the arguments.',
        callApi:
          'Executes the operation against the backend. The body of the response is returned verbatim for the LLM to reason about.'
      }
    }
  },
  registries: {
    title: 'Service Registry',
    subtitle:
      'Configure Nacos / Eureka registries. Once enabled, the gateway polls every 5 minutes and ' +
      'turns any newly discovered service (and its OpenAPI / Swagger document) into MCP tools.',
    addButton: 'Add Registry',
    columns: {
      name: 'Name',
      type: 'Type',
      endpoint: 'Endpoint',
      env: 'Env',
      services: 'Services',
      status: 'Status',
      lastSynced: 'Last synced',
      enabled: 'Enabled'
    },
    status: {
      UNKNOWN: 'UNKNOWN',
      OK: 'OK',
      ERROR: 'ERROR'
    },
    actions: {
      test: 'Test',
      discover: 'Discover Now',
      edit: 'Edit',
      delete: 'Delete'
    },
    empty: {
      description: 'No registries configured yet',
      cta: 'Add Registry'
    },
    deleteConfirm: {
      title: 'Confirm deletion',
      message: 'Delete registry "{name}"? Services previously imported from it stay intact.',
      confirmText: 'Delete',
      cancelText: 'Cancel'
    },
    messages: {
      created: 'Registry "{name}" created',
      updated: 'Registry updated',
      deleted: 'Registry "{name}" deleted',
      testOk: 'Connection ok ({ms} ms)',
      testFail: 'Connection failed: {reason}',
      discoveryOk: '"{name}": {discovered} found, {imported} imported, {skipped} skipped',
      nameRequired: 'Name is required',
      endpointRequired: 'Endpoint is required',
      stubNotImpl: 'This type is a roadmap placeholder; cannot create yet.'
    },
    typeBadge: {
      preview: 'preview'
    },
    dialog: {
      createTitle: 'Add Registry',
      editTitle: 'Edit Registry',
      fields: {
        name: 'Name',
        displayName: 'Display name',
        type: 'Type',
        endpoint: 'Endpoint',
        username: 'Username',
        password: 'Password',
        environment: 'Default environment',
        namespace: 'Namespace',
        groupName: 'Group',
        extra: 'Extra options',
        enabled: 'Enabled'
      },
      placeholders: {
        name: 'nacos-prod',
        displayName: 'Nacos prod cluster',
        endpoint: '127.0.0.1:8848 or http://eureka:8761',
        username: 'optional',
        password: 'optional; leave blank to keep current',
        namespace: 'Nacos namespace id; leave blank for Eureka',
        groupName: 'Nacos group; defaults to DEFAULT_GROUP',
        extra: 'k1=v1;k2=v2 form, e.g. preferIpAddress=true'
      },
      hints: {
        envHint: 'Every service discovered through this registry is tagged with this environment.',
        passwordHint: 'On edit, leave blank to keep current; submit empty string to clear.'
      },
      submit: 'Save'
    },
    discoveryDialog: {
      title: 'Discovery result: {name}',
      summary: '{discovered} found, {imported} imported, {skipped} skipped',
      columns: {
        name: 'Service',
        baseUrl: 'Probed URL',
        status: 'Status',
        message: 'Detail'
      },
      itemStatus: {
        IMPORTED: 'IMPORTED',
        UPDATED: 'UPDATED',
        SKIPPED: 'SKIPPED'
      },
      close: 'Close'
    }
  },

  exposure: {
    currentLabel: 'Active exposure mode',
    changeButton: 'Change mode',
    dialogTitle: 'Change exposure mode',
    saveButton: 'Save',
    recommend: 'Recommended',
    noteLabel: 'Note',
    notePlaceholder: 'Optional: record why you changed the mode so audit can trace it later',
    confirmTitle: 'Switch to DIRECT_ALL?',
    confirmDirectAll:
      'DIRECT_ALL exposes all {count} tools directly to the LLM. With many tools the LLM\'s context budget can blow up; recommended only for demos or single-service deployments. Continue?',
    confirmButton: 'Switch anyway',
    updated: 'Mode switched to {mode}',
    stats: {
      effective: 'Exposed',
      total: 'Total tools',
      promoted: 'Promoted',
      meta: 'Meta tools'
    },
    modes: {
      META: {
        label: 'META (meta tools only)',
        summary:
          'Only the four meta tools are advertised. The LLM reaches every backend operation through search_api → call_api.',
        desc: 'Only the four meta tools are advertised; real operations stay hidden behind them.'
      },
      HYBRID: {
        label: 'HYBRID (recommended)',
        summary:
          'Meta tools plus operator-promoted high-traffic operations. Balanced for discoverability and context budget.',
        desc: 'Meta tools plus promoted operations are exposed directly.'
      },
      DIRECT_ALL: {
        label: 'DIRECT_ALL (everything direct)',
        summary:
          'Every non-deprecated operation is exposed as a first-class tool; meta tools are dropped.',
        desc: 'Every operation is exposed directly; meta tools are dropped.',
        warn:
          '⚠️ With more than ~30 tools the LLM context budget can blow up. Recommended only for demos / single-service deployments. Production: stay on HYBRID.'
      }
    }
  },

  placeholder: {
    pending: 'This feature lands in {milestone}',
    description:
      'The page is wired up early so the product roadmap is visible end-to-end. The backend will be filled in by the indicated milestone.'
  },

  audit: {
    title: 'Audit log',
    subtitle:
      'A trail of write operations and MCP invocations. B5 ships a synthetic in-memory feed; persistence and archival arrive in a later milestone.',
    filters: {
      keywordPlaceholder: 'action, actor, resource',
      outcomeAll: 'All outcomes',
      outcomeSuccess: 'Success',
      outcomeFailure: 'Failure'
    },
    columns: {
      timestamp: 'Time',
      actor: 'Actor',
      action: 'Action',
      resource: 'Resource',
      outcome: 'Outcome',
      status: 'HTTP',
      duration: 'Duration',
      detail: 'Detail'
    },
    outcome: {
      SUCCESS: 'Success',
      FAILURE: 'Failure'
    },
    drawer: {
      title: 'Event detail',
      sections: {
        basic: 'Basic',
        client: 'Client',
        detail: 'Detail'
      },
      labels: {
        id: 'ID',
        timestamp: 'Timestamp',
        actor: 'Actor',
        action: 'Action',
        resourceType: 'Resource type',
        resourceId: 'Resource id',
        outcome: 'Outcome',
        status: 'HTTP status',
        duration: 'Duration (ms)',
        clientIp: 'Client IP',
        userAgent: 'User-Agent'
      }
    },
    empty: 'No matching audit events'
  },

  policies: {
    title: 'Governance policies',
    subtitle:
      'High-fidelity policy cards: toggle on/off, tune severity, edit JSON config. Backend CRUD ships in B5; runtime evaluation hooks land with the governance engine.',
    filters: {
      keywordPlaceholder: 'Search policies',
      categoryAll: 'All categories',
      statusAll: 'All statuses',
      statusEnabled: 'Enabled',
      statusDisabled: 'Disabled'
    },
    severity: {
      LOW: 'Low',
      MEDIUM: 'Medium',
      HIGH: 'High',
      CRITICAL: 'Critical'
    },
    category: {
      governance: 'Governance',
      traffic: 'Traffic',
      compliance: 'Compliance',
      auth: 'Auth'
    },
    actions: {
      enable: 'Enable',
      disable: 'Disable',
      edit: 'Edit config'
    },
    editDialog: {
      title: 'Edit policy: {name}',
      fields: {
        enabled: 'Enabled',
        severity: 'Severity',
        configJson: 'Config (JSON)',
        note: 'Change note (optional)'
      },
      configHint: 'Validated as JSON before saving',
      jsonInvalid: 'Config is not valid JSON',
      submit: 'Save',
      cancel: 'Cancel'
    },
    messages: {
      toggled: '"{name}" {status}',
      updated: '"{name}" config updated'
    },
    empty: 'No matching policies'
  },

  health: {
    title: 'Tool health',
    subtitle:
      'Aggregated success rate, call volume and latency distribution per MCP tool. B5 uses a deterministic generator (for screenshots and demos); a real metrics backend lands later.',
    kpis: {
      totalTools: 'Total tools',
      activeTools: 'Active tools',
      calls24h: 'Calls (24h)',
      successRate: 'Success rate',
      avgLatency: 'Avg latency'
    },
    charts: {
      callVolumeTitle: 'Call volume (24h)',
      callVolumeSuccess: 'Success',
      callVolumeFailure: 'Failure',
      latencyTitle: 'P50 latency distribution',
      latencyAxis: 'Calls'
    },
    table: {
      title: 'Top tools',
      columns: {
        toolName: 'Tool',
        service: 'Service',
        calls24h: 'Calls (24h)',
        successRate: 'Success',
        p50: 'P50',
        p95: 'P95',
        p99: 'P99',
        lastInvoked: 'Last invoked',
        lastError: 'Last error'
      }
    },
    empty: 'No tools yet - import an OpenAPI document from the Services page first.'
  },

  settings: {
    title: 'Settings',
    subtitle: 'Site-wide preferences, shared by every node.',
    sections: {
      general: 'General',
      schedule: 'Schedule',
      governance: 'Governance',
      demo: 'Demo'
    },
    fields: {
      siteName: 'Site name',
      defaultEnvironment: 'Default environment',
      refreshCron: 'Swagger refresh cron',
      maxToolsPerService: 'Max tools per service',
      ssoEnabled: 'Enable SSO',
      auditRetentionDays: 'Audit retention (days)',
      demoMode: 'Keep demo data',
      updatedAt: 'Last updated'
    },
    hints: {
      cron: 'Six-field Spring cron, e.g. 0 0 3 * * * (daily at 03:00)',
      maxTools: 'Hard cap to protect the gateway from pathological specs declaring thousands of operations.',
      sso: 'Off in the OSS demo profile; flip on after wiring up enterprise SSO.',
      demo: 'When off, the demo seed runner stops re-populating sample services on startup.'
    },
    submit: 'Save',
    saved: 'Settings saved'
  },

  errors: {
    networkError: 'Network error',
    notFound: 'Resource not found'
  }
};
