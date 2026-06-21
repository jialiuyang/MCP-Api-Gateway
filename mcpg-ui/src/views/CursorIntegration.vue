<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const origin = window.location.origin;
const httpUrl = computed(() => `${origin}/mcp`);
const sseUrl = computed(() => `${origin}/mcp/sse`);

const httpConfig = computed(() =>
  JSON.stringify(
    {
      mcpServers: {
        'mcp-gateway-enterprise': {
          url: httpUrl.value
        }
      }
    },
    null,
    2
  )
);

const sseConfig = computed(() =>
  JSON.stringify(
    {
      mcpServers: {
        'mcp-gateway-enterprise': {
          type: 'sse',
          url: sseUrl.value
        }
      }
    },
    null,
    2
  )
);

const stdioConfig = computed(() =>
  JSON.stringify(
    {
      mcpServers: {
        'mcp-gateway-enterprise': {
          command: 'java',
          args: ['-jar', '/absolute/path/to/mcpg-stdio-bridge.jar', '--url', httpUrl.value]
        }
      }
    },
    null,
    2
  )
);

const activeTransport = ref<'http' | 'sse' | 'stdio'>('http');

async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(t('common.copied'));
  } catch {
    ElMessage.error(t('common.clipboardError'));
  }
}
</script>

<template>
  <div class="cursor-page">
    <el-card shadow="never" class="page-header">
      <template #header>
        <div>
          <h2 class="title">{{ t('cursor.title') }}</h2>
          <p class="subtitle">
            <i18n-t keypath="cursor.subtitle">
              <template #listServices><code>list_services</code></template>
              <template #searchApi><code>search_api</code></template>
              <template #getApiSchema><code>get_api_schema</code></template>
              <template #callApi><code>call_api</code></template>
            </i18n-t>
          </p>
        </div>
      </template>

      <div class="endpoint-block">
        <span class="endpoint-label">{{ t('cursor.endpoints.httpLabel') }}</span>
        <code class="endpoint-value">{{ httpUrl }}</code>
        <el-button size="small" @click="copyToClipboard(httpUrl)">
          {{ t('cursor.endpoints.copyUrl') }}
        </el-button>
      </div>
      <div class="endpoint-block">
        <span class="endpoint-label">{{ t('cursor.endpoints.sseLabel') }}</span>
        <code class="endpoint-value">{{ sseUrl }}</code>
        <el-button size="small" @click="copyToClipboard(sseUrl)">
          {{ t('cursor.endpoints.copyUrl') }}
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeTransport">
        <el-tab-pane :label="t('cursor.tabs.http')" name="http">
          <p>{{ t('cursor.http.intro') }}</p>

          <h4>{{ t('cursor.http.step1Title') }}</h4>
          <p>{{ t('cursor.http.step1Body') }}</p>
          <ul class="client-list">
            <li>
              <i18n-t keypath="cursor.http.step1Files.cursor">
                <template #path><code>~/.cursor/mcp.json</code></template>
                <template #winPath><code>%USERPROFILE%\.cursor\mcp.json</code></template>
              </i18n-t>
            </li>
            <li>
              <i18n-t keypath="cursor.http.step1Files.claude">
                <template #path><code>~/Library/Application Support/Claude/claude_desktop_config.json</code></template>
                <template #winPath><code>%APPDATA%\Claude\claude_desktop_config.json</code></template>
              </i18n-t>
            </li>
            <li>
              <i18n-t keypath="cursor.http.step1Files.cline">
                <template #path><code>cline_mcp_settings.json</code> (VS Code settings UI)</template>
              </i18n-t>
            </li>
            <li>
              <i18n-t keypath="cursor.http.step1Files.windsurf">
                <template #path><code>~/.codeium/windsurf/mcp_config.json</code></template>
              </i18n-t>
            </li>
          </ul>
          <pre class="code-block">{{ httpConfig }}</pre>
          <el-button type="primary" size="small" @click="copyToClipboard(httpConfig)">
            {{ t('cursor.http.copyConfig') }}
          </el-button>

          <h4>{{ t('cursor.http.step2Title') }}</h4>
          <p>
            <i18n-t keypath="cursor.http.step2Body">
              <template #listServices><code>list_services</code></template>
              <template #searchApi><code>search_api</code></template>
              <template #getApiSchema><code>get_api_schema</code></template>
              <template #callApi><code>call_api</code></template>
            </i18n-t>
          </p>

          <h4>{{ t('cursor.http.step3Title') }}</h4>
          <p>{{ t('cursor.http.step3Body') }}</p>
          <pre class="code-block">{{ t('cursor.http.step3Example') }}</pre>
        </el-tab-pane>

        <el-tab-pane :label="t('cursor.tabs.sse')" name="sse">
          <el-alert type="info" :closable="false" :title="t('cursor.sse.hint')" />
          <h4>{{ t('cursor.sse.configTitle') }}</h4>
          <pre class="code-block">{{ sseConfig }}</pre>
          <el-button type="primary" size="small" @click="copyToClipboard(sseConfig)">
            {{ t('cursor.sse.copyConfig') }}
          </el-button>
          <p class="hint">
            <i18n-t keypath="cursor.sse.explainer">
              <template #typeFlag><code>"type": "sse"</code></template>
              <template #sse><code>/mcp/sse</code></template>
            </i18n-t>
          </p>
        </el-tab-pane>

        <el-tab-pane :label="t('cursor.tabs.stdio')" name="stdio">
          <el-alert type="info" :closable="false" :title="t('cursor.stdio.intro')" />

          <h4>{{ t('cursor.stdio.step1Title') }}</h4>
          <pre class="code-block">cd mcp-gateway-enterprise
mvn -pl mcpg-stdio-bridge -am package -DskipTests</pre>

          <h4>{{ t('cursor.stdio.step2Title') }}</h4>
          <pre class="code-block">{{ stdioConfig }}</pre>
          <el-button type="primary" size="small" @click="copyToClipboard(stdioConfig)">
            {{ t('cursor.stdio.copyConfig') }}
          </el-button>

          <p class="hint">
            <i18n-t keypath="cursor.stdio.hint1">
              <template #placeholder><code>/absolute/path/to/mcpg-stdio-bridge.jar</code></template>
              <template #defaultPath><code>mcpg-stdio-bridge/target/mcpg-stdio-bridge-*.jar</code></template>
            </i18n-t>
          </p>
          <p class="hint">
            <i18n-t keypath="cursor.stdio.hint2">
              <template #flag><code>--url</code></template>
              <template #endpoint><code>/mcp</code></template>
            </i18n-t>
          </p>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <h3 class="section-title">{{ t('cursor.metaTools.title') }}</h3>
      </template>
      <el-steps direction="vertical" :active="4" finish-status="success">
        <el-step
          title="list_services"
          :description="t('cursor.metaTools.steps.listServices')"
        />
        <el-step
          title="search_api(keyword)"
          :description="t('cursor.metaTools.steps.searchApi')"
        />
        <el-step
          title="get_api_schema(tool_name)"
          :description="t('cursor.metaTools.steps.getApiSchema')"
        />
        <el-step
          title="call_api(tool_name, arguments)"
          :description="t('cursor.metaTools.steps.callApi')"
        />
      </el-steps>
    </el-card>
  </div>
</template>

<style scoped>
.cursor-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-header .title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}
.page-header .subtitle {
  margin: 6px 0 0 0;
  color: var(--el-text-color-secondary);
}
.page-header .subtitle code {
  background: var(--el-fill-color-light);
  padding: 1px 6px;
  border-radius: 4px;
}
.endpoint-block {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  margin-bottom: 8px;
}
.endpoint-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.endpoint-value {
  flex: 1;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  word-break: break-all;
}
.code-block {
  background: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 6px;
  overflow: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  white-space: pre;
}
.section-title {
  margin: 0;
  font-size: 16px;
}
h4 {
  margin: 16px 0 8px 0;
}
.hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.client-list {
  margin: 8px 0 12px 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--el-text-color-regular);
}
.client-list code {
  background: var(--el-fill-color-light);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
}
</style>
