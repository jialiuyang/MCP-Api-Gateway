<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { toolsApi, type ToolDto, type RiskLevel } from '@/api/tools';
import { servicesApi, type ServiceDto } from '@/api/services';
import {
  exposureApi,
  type ExposureMode,
  type ExposureSettingsDto
} from '@/api/exposure';

const { t } = useI18n();

const loading = ref(false);
const tools = ref<ToolDto[]>([]);
const services = ref<ServiceDto[]>([]);
const filters = reactive<{ keyword: string; serviceId: number | '' }>({
  keyword: '',
  serviceId: ''
});

const exposure = ref<ExposureSettingsDto | null>(null);
const exposureDialogVisible = ref(false);
const exposureDialogSubmitting = ref(false);
const exposureForm = reactive<{ mode: ExposureMode; note: string }>({
  mode: 'HYBRID',
  note: ''
});

const drawerOpen = ref(false);
const drawerLoading = ref(false);
const selected = ref<ToolDto | null>(null);

const filtered = computed(() => tools.value);

/** Tag color per exposure mode, matching the severity vibe. */
function exposureTagType(mode: ExposureMode | undefined) {
  switch (mode) {
    case 'META':
      return 'info';
    case 'HYBRID':
      return 'success';
    case 'DIRECT_ALL':
      return 'warning';
    default:
      return 'info';
  }
}

async function fetchExposure() {
  try {
    exposure.value = await exposureApi.get();
  } catch (err) {
    ElMessage.error((err as Error).message);
  }
}

function openExposureDialog() {
  if (!exposure.value) return;
  exposureForm.mode = exposure.value.mode;
  exposureForm.note = exposure.value.note ?? '';
  exposureDialogVisible.value = true;
}

async function submitExposure() {
  if (!exposure.value) return;
  if (
    exposureForm.mode === 'DIRECT_ALL' &&
    exposure.value.mode !== 'DIRECT_ALL'
  ) {
    try {
      await ElMessageBox.confirm(
        t('exposure.confirmDirectAll', { count: exposure.value.totalTools }),
        t('exposure.confirmTitle'),
        {
          confirmButtonText: t('exposure.confirmButton'),
          cancelButtonText: t('common.cancel'),
          type: 'warning'
        }
      );
    } catch (e) {
      if (e === 'cancel') return;
      throw e;
    }
  }
  exposureDialogSubmitting.value = true;
  try {
    exposure.value = await exposureApi.update({
      mode: exposureForm.mode,
      note: exposureForm.note?.trim() || undefined
    });
    ElMessage.success(
      t('exposure.updated', {
        mode: t(`exposure.modes.${exposureForm.mode}.label`)
      })
    );
    exposureDialogVisible.value = false;
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    exposureDialogSubmitting.value = false;
  }
}

async function fetchTools() {
  loading.value = true;
  try {
    tools.value = await toolsApi.list({
      keyword: filters.keyword || undefined,
      serviceId: filters.serviceId || undefined
    });
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    loading.value = false;
  }
}

async function fetchServices() {
  try {
    services.value = await servicesApi.list();
  } catch {
    // non-fatal; service select stays empty
  }
}

async function openDetail(row: unknown) {
  const tool = row as ToolDto;
  drawerOpen.value = true;
  drawerLoading.value = true;
  try {
    selected.value = await toolsApi.get(tool.id);
  } catch (err) {
    ElMessage.error((err as Error).message);
    drawerOpen.value = false;
  } finally {
    drawerLoading.value = false;
  }
}

async function togglePromote(row: ToolDto) {
  try {
    const next = !row.promoted;
    const updated = await toolsApi.promote(row.id, next);
    row.promoted = updated.promoted;
    ElMessage.success(
      next
        ? t('tools.promote.promoted', { name: row.toolName })
        : t('tools.promote.demoted', { name: row.toolName })
    );
  } catch (err) {
    ElMessage.error((err as Error).message);
  }
}

function methodColor(method: string) {
  switch (method.toUpperCase()) {
    case 'GET':
      return 'success';
    case 'POST':
      return 'primary';
    case 'PUT':
    case 'PATCH':
      return 'warning';
    case 'DELETE':
      return 'danger';
    default:
      return 'info';
  }
}

function riskColor(level: RiskLevel) {
  switch (level) {
    case 'READ':
      return 'success';
    case 'WRITE_LOW':
      return 'warning';
    case 'WRITE_HIGH':
      return 'danger';
    case 'FORBIDDEN':
      return 'danger';
    default:
      return 'info';
  }
}

function serviceName(serviceId: number): string {
  const found = services.value.find((s) => s.id === serviceId);
  return found ? found.name : '#' + serviceId;
}

function prettyJson(value: unknown): string {
  if (value === null || value === undefined) return '—';
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

onMounted(async () => {
  await Promise.all([fetchTools(), fetchServices(), fetchExposure()]);
});
</script>

<template>
  <div class="tools-page">
    <el-card shadow="never" class="exposure-card">
      <div class="exposure-row">
        <div class="exposure-left">
          <div class="exposure-label">{{ t('exposure.currentLabel') }}</div>
          <div class="exposure-mode">
            <el-tag
              v-if="exposure"
              size="large"
              :type="exposureTagType(exposure.mode)"
              effect="dark"
            >
              {{ t(`exposure.modes.${exposure.mode}.label`) }}
            </el-tag>
            <el-skeleton v-else animated style="width: 120px">
              <template #template><el-skeleton-item variant="text" style="width: 100%" /></template>
            </el-skeleton>
          </div>
          <div class="exposure-desc">
            {{ exposure ? t(`exposure.modes.${exposure.mode}.desc`) : '' }}
          </div>
        </div>
        <div class="exposure-stats" v-if="exposure">
          <div class="stat">
            <div class="stat-value">{{ exposure.effectiveCount }}</div>
            <div class="stat-label">{{ t('exposure.stats.effective') }}</div>
          </div>
          <el-divider direction="vertical" />
          <div class="stat">
            <div class="stat-value">{{ exposure.totalTools }}</div>
            <div class="stat-label">{{ t('exposure.stats.total') }}</div>
          </div>
          <el-divider direction="vertical" />
          <div class="stat">
            <div class="stat-value">{{ exposure.promotedTools }}</div>
            <div class="stat-label">{{ t('exposure.stats.promoted') }}</div>
          </div>
          <el-divider direction="vertical" />
          <div class="stat">
            <div class="stat-value">{{ exposure.metaToolCount }}</div>
            <div class="stat-label">{{ t('exposure.stats.meta') }}</div>
          </div>
        </div>
        <div class="exposure-right">
          <el-button type="primary" :disabled="!exposure" @click="openExposureDialog">
            {{ t('exposure.changeButton') }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="page-header">
      <template #header>
        <div>
          <h2 class="title">{{ t('tools.title') }}</h2>
          <p class="subtitle">
            <i18n-t keypath="tools.subtitle">
              <template #listServices><code>list_services</code></template>
              <template #searchApi><code>search_api</code></template>
              <template #getApiSchema><code>get_api_schema</code></template>
              <template #callApi><code>call_api</code></template>
            </i18n-t>
          </p>
        </div>
      </template>

      <el-form :inline="true" class="filters">
        <el-form-item :label="t('common.search')">
          <el-input
            v-model="filters.keyword"
            :placeholder="t('tools.filters.keywordPlaceholder')"
            clearable
            style="width: 260px"
            @keyup.enter="fetchTools"
            @clear="fetchTools"
          />
        </el-form-item>
        <el-form-item :label="t('tools.columns.service')">
          <el-select
            v-model="filters.serviceId"
            :placeholder="t('tools.filters.servicePlaceholder')"
            clearable
            filterable
            style="width: 220px"
            @change="fetchTools"
            @clear="fetchTools"
          >
            <el-option
              v-for="s in services"
              :key="s.id"
              :value="s.id"
              :label="`${s.name} (${s.environment})`"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :icon="'Search'" @click="fetchTools">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="filtered"
        stripe
        @row-click="openDetail"
      >
        <el-table-column :label="t('tools.columns.toolName')" min-width="220" prop="toolName">
          <template #default="{ row }">
            <code>{{ row.toolName }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('tools.columns.service')" width="160">
          <template #default="{ row }">{{ serviceName(row.serviceId) }}</template>
        </el-table-column>
        <el-table-column :label="t('tools.columns.method')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="methodColor(row.httpMethod)">
              {{ row.httpMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('tools.columns.path')" prop="path" min-width="220" show-overflow-tooltip />
        <el-table-column :label="t('tools.columns.summary')" prop="summary" min-width="220" show-overflow-tooltip />
        <el-table-column :label="t('tools.columns.risk')" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="riskColor(row.riskLevel)">
              {{ t(`tools.risk.${row.riskLevel}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('tools.columns.promoted')" width="150" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="(row as ToolDto).promoted"
              @click.stop
              @change="togglePromote(row as ToolDto)"
            />
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && filtered.length === 0"
        :description="t('tools.empty.description')"
      >
        <router-link to="/services">
          <el-button type="primary">{{ t('tools.empty.cta') }}</el-button>
        </router-link>
      </el-empty>
    </el-card>

    <el-dialog
      v-model="exposureDialogVisible"
      :title="t('exposure.dialogTitle')"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-radio-group v-model="exposureForm.mode" class="mode-radio-group">
        <el-radio
          v-for="mode in (['META', 'HYBRID', 'DIRECT_ALL'] as ExposureMode[])"
          :key="mode"
          :value="mode"
          class="mode-radio"
        >
          <div class="mode-content">
            <div class="mode-title">
              <el-tag size="small" :type="exposureTagType(mode)" effect="dark">
                {{ t(`exposure.modes.${mode}.label`) }}
              </el-tag>
              <span class="mode-recommend" v-if="mode === 'HYBRID'">
                {{ t('exposure.recommend') }}
              </span>
            </div>
            <div class="mode-summary">{{ t(`exposure.modes.${mode}.summary`) }}</div>
            <div class="mode-warn" v-if="mode === 'DIRECT_ALL'">
              {{ t('exposure.modes.DIRECT_ALL.warn') }}
            </div>
          </div>
        </el-radio>
      </el-radio-group>

      <el-form label-width="100px" label-position="right" class="exposure-note-form">
        <el-form-item :label="t('exposure.noteLabel')">
          <el-input
            v-model="exposureForm.note"
            type="textarea"
            :rows="2"
            :placeholder="t('exposure.notePlaceholder')"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="exposureDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="exposureDialogSubmitting"
          @click="submitExposure"
        >
          {{ t('exposure.saveButton') }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="drawerOpen"
      :title="t('tools.drawer.title')"
      size="640px"
      :destroy-on-close="true"
    >
      <div v-loading="drawerLoading" class="drawer-body">
        <template v-if="selected">
          <div class="drawer-header">
            <code class="drawer-tool-name">{{ selected.toolName }}</code>
            <div class="drawer-tags">
              <el-tag :type="methodColor(selected.httpMethod)">{{ selected.httpMethod }}</el-tag>
              <el-tag :type="riskColor(selected.riskLevel)">
                {{ t(`tools.risk.${selected.riskLevel}`) }}
              </el-tag>
              <el-tag v-if="selected.promoted" type="success">
                {{ t('tools.columns.promoted') }}
              </el-tag>
              <el-tag v-if="selected.deprecated" type="danger">
                {{ t('tools.drawer.deprecated') }}
              </el-tag>
            </div>
          </div>

          <el-descriptions :column="1" border>
            <el-descriptions-item :label="t('tools.drawer.labels.path')">
              <code>{{ selected.path }}</code>
            </el-descriptions-item>
            <el-descriptions-item :label="t('tools.drawer.labels.service')">
              {{ serviceName(selected.serviceId) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('tools.drawer.labels.operationId')">
              {{ selected.operationId }}
            </el-descriptions-item>
            <el-descriptions-item v-if="selected.summary" :label="t('tools.drawer.labels.summary')">
              {{ selected.summary }}
            </el-descriptions-item>
            <el-descriptions-item v-if="selected.tags && selected.tags.length" :label="t('tools.drawer.labels.tags')">
              <el-tag v-for="tag in selected.tags" :key="tag" size="small" class="tag-chip">
                {{ tag }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="selected.description" class="section">
            <h4>{{ t('tools.drawer.sections.description') }}</h4>
            <pre class="description-block">{{ selected.description }}</pre>
          </div>

          <div class="section">
            <h4>{{ t('tools.drawer.sections.inputSchema') }}</h4>
            <pre class="json-block">{{ prettyJson(selected.inputSchema) }}</pre>
          </div>

          <div v-if="selected.outputSchema" class="section">
            <h4>{{ t('tools.drawer.sections.outputSchema') }}</h4>
            <pre class="json-block">{{ prettyJson(selected.outputSchema) }}</pre>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.tools-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.exposure-card :deep(.el-card__body) {
  padding: 18px 20px;
}
.exposure-row {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}
.exposure-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 240px;
  flex: 1;
}
.exposure-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.exposure-mode {
  display: flex;
  align-items: center;
}
.exposure-desc {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.exposure-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}
.exposure-stats .stat {
  text-align: center;
  min-width: 56px;
}
.exposure-stats .stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1;
}
.exposure-stats .stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.exposure-right {
  display: flex;
  align-items: center;
}
.mode-radio-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}
.mode-radio {
  align-items: flex-start !important;
  padding: 10px 14px;
  margin-right: 0 !important;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  width: 100%;
  height: auto;
}
.mode-radio :deep(.el-radio__label) {
  flex: 1;
  white-space: normal;
  padding-left: 8px;
}
.mode-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.mode-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mode-recommend {
  font-size: 12px;
  color: var(--el-color-success);
  font-weight: 500;
}
.mode-summary {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}
.mode-warn {
  font-size: 12px;
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);
  padding: 6px 10px;
  border-radius: 4px;
  margin-top: 4px;
}
.exposure-note-form {
  margin-top: 14px;
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
.filters {
  margin-top: 4px;
}
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.drawer-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.drawer-tool-name {
  font-size: 18px;
}
.drawer-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.section h4 {
  margin: 8px 0;
}
.description-block,
.json-block {
  background: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 6px;
  overflow: auto;
  max-height: 360px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
.tag-chip {
  margin-right: 4px;
}
</style>
