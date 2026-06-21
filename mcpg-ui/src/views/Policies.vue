<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import {
  policiesApi,
  type PolicyDto,
  type PolicySeverity,
  type UpdatePolicyRequest
} from '@/api/policies';

const { t, te } = useI18n();

const loading = ref(false);
const policies = ref<PolicyDto[]>([]);

const filters = reactive<{
  keyword: string;
  category: string;
  status: '' | 'enabled' | 'disabled';
}>({
  keyword: '',
  category: '',
  status: ''
});

const editDialog = reactive<{
  visible: boolean;
  row: PolicyDto | null;
  enabled: boolean;
  severity: PolicySeverity;
  configJson: string;
  note: string;
  saving: boolean;
}>({
  visible: false,
  row: null,
  enabled: true,
  severity: 'MEDIUM',
  configJson: '',
  note: '',
  saving: false
});

const severityOptions: PolicySeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const categories = computed(() => {
  const seen = new Set<string>();
  policies.value.forEach((p) => seen.add(p.category));
  return Array.from(seen).sort();
});

const filtered = computed(() => {
  const kw = filters.keyword.trim().toLowerCase();
  return policies.value.filter((p) => {
    if (filters.category && p.category !== filters.category) return false;
    if (filters.status === 'enabled' && !p.enabled) return false;
    if (filters.status === 'disabled' && p.enabled) return false;
    if (!kw) return true;
    return (
      p.name.toLowerCase().includes(kw) ||
      p.policyKey.toLowerCase().includes(kw) ||
      (p.description ?? '').toLowerCase().includes(kw)
    );
  });
});

function severityTag(s: PolicySeverity): 'success' | 'info' | 'warning' | 'danger' {
  switch (s) {
    case 'LOW':
      return 'info';
    case 'MEDIUM':
      return 'success';
    case 'HIGH':
      return 'warning';
    case 'CRITICAL':
      return 'danger';
  }
}

function categoryLabel(category: string): string {
  const key = `policies.category.${category}`;
  return te(key) ? t(key) : category;
}

async function fetchPolicies() {
  loading.value = true;
  try {
    policies.value = await policiesApi.list();
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    loading.value = false;
  }
}

async function toggle(p: PolicyDto) {
  const req: UpdatePolicyRequest = { enabled: !p.enabled };
  try {
    const updated = await policiesApi.update(p.id, req);
    const idx = policies.value.findIndex((x) => x.id === updated.id);
    if (idx !== -1) policies.value[idx] = updated;
    ElMessage.success(
      t('policies.messages.toggled', {
        name: updated.name,
        status: updated.enabled ? t('policies.actions.enable') : t('policies.actions.disable')
      })
    );
  } catch (err) {
    ElMessage.error((err as Error).message);
  }
}

function openEdit(p: PolicyDto) {
  editDialog.row = p;
  editDialog.enabled = p.enabled;
  editDialog.severity = p.severity;
  editDialog.configJson = formatJson(p.configJson ?? '');
  editDialog.note = '';
  editDialog.visible = true;
}

function formatJson(raw: string): string {
  const t = raw.trim();
  if (!t) return '';
  try {
    return JSON.stringify(JSON.parse(t), null, 2);
  } catch {
    return raw;
  }
}

function isJson(s: string): boolean {
  const t = s.trim();
  if (!t) return true;
  try {
    JSON.parse(t);
    return true;
  } catch {
    return false;
  }
}

async function submitEdit() {
  if (!editDialog.row) return;
  if (!isJson(editDialog.configJson)) {
    ElMessage.error(t('policies.editDialog.jsonInvalid'));
    return;
  }
  editDialog.saving = true;
  try {
    const updated = await policiesApi.update(editDialog.row.id, {
      enabled: editDialog.enabled,
      severity: editDialog.severity,
      configJson: editDialog.configJson || undefined,
      note: editDialog.note || undefined
    });
    const idx = policies.value.findIndex((x) => x.id === updated.id);
    if (idx !== -1) policies.value[idx] = updated;
    ElMessage.success(t('policies.messages.updated', { name: updated.name }));
    editDialog.visible = false;
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    editDialog.saving = false;
  }
}

onMounted(fetchPolicies);
</script>

<template>
  <div class="mcpg-page" v-loading="loading">
    <header class="header-row">
      <div class="title-block">
        <h2>{{ t('policies.title') }}</h2>
        <p>{{ t('policies.subtitle') }}</p>
      </div>
    </header>

    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item>
          <el-input
            v-model="filters.keyword"
            :placeholder="t('policies.filters.keywordPlaceholder')"
            clearable
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="filters.category"
            :placeholder="t('policies.filters.categoryAll')"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="c in categories"
              :key="c"
              :label="categoryLabel(c)"
              :value="c"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="filters.status"
            :placeholder="t('policies.filters.statusAll')"
            clearable
            style="width: 160px"
          >
            <el-option :label="t('policies.filters.statusEnabled')" value="enabled" />
            <el-option :label="t('policies.filters.statusDisabled')" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="fetchPolicies">{{ t('common.refresh') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-if="filtered.length === 0 && !loading" class="empty">
      <el-empty :description="t('policies.empty')" />
    </div>

    <div v-else class="grid">
      <el-card
        v-for="p in filtered"
        :key="p.id"
        class="policy-card"
        :class="{ 'is-disabled': !p.enabled }"
        shadow="hover"
      >
        <div class="card-header">
          <div class="left">
            <el-tag :type="severityTag(p.severity)" effect="dark" size="small" class="sev">
              {{ t(`policies.severity.${p.severity}`) }}
            </el-tag>
            <el-tag size="small" type="info" effect="plain">{{ categoryLabel(p.category) }}</el-tag>
          </div>
          <el-switch
            :model-value="p.enabled"
            @change="toggle(p)"
            :active-text="t('policies.actions.enable')"
            :inactive-text="t('policies.actions.disable')"
            inline-prompt
          />
        </div>
        <h3 class="card-title">{{ p.name }}</h3>
        <p class="card-key">
          <code>{{ p.policyKey }}</code>
        </p>
        <p class="card-desc">{{ p.description }}</p>
        <div class="card-footer">
          <span class="updated">
            {{ new Date(p.updatedAt).toLocaleString() }}
          </span>
          <el-button size="small" link type="primary" @click="openEdit(p)">
            {{ t('policies.actions.edit') }}
          </el-button>
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="editDialog.visible"
      :title="t('policies.editDialog.title', { name: editDialog.row?.name ?? '' })"
      width="640px"
    >
      <el-form label-position="top">
        <el-form-item :label="t('policies.editDialog.fields.enabled')">
          <el-switch v-model="editDialog.enabled" />
        </el-form-item>
        <el-form-item :label="t('policies.editDialog.fields.severity')">
          <el-radio-group v-model="editDialog.severity">
            <el-radio-button
              v-for="s in severityOptions"
              :key="s"
              :value="s"
            >
              {{ t(`policies.severity.${s}`) }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('policies.editDialog.fields.configJson')">
          <el-input
            v-model="editDialog.configJson"
            type="textarea"
            :rows="8"
            class="config-textarea"
            spellcheck="false"
          />
          <p class="hint">{{ t('policies.editDialog.configHint') }}</p>
        </el-form-item>
        <el-form-item :label="t('policies.editDialog.fields.note')">
          <el-input v-model="editDialog.note" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">{{ t('policies.editDialog.cancel') }}</el-button>
        <el-button type="primary" :loading="editDialog.saving" @click="submitEdit">
          {{ t('policies.editDialog.submit') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.title-block h2 {
  margin: 0;
}
.title-block p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  max-width: 920px;
}
.filter-card {
  margin-bottom: 16px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.policy-card {
  transition: opacity 0.2s ease;
}
.policy-card.is-disabled {
  opacity: 0.6;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.card-header .left {
  display: flex;
  gap: 6px;
}
.sev {
  font-weight: 600;
}
.card-title {
  margin: 12px 0 4px;
  font-size: 16px;
}
.card-key {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.card-key code {
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
}
.card-desc {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-regular);
  min-height: 60px;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 8px;
  margin-top: 4px;
}
.updated {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.config-textarea :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
.hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.empty {
  padding: 80px 0;
  text-align: center;
}
</style>
