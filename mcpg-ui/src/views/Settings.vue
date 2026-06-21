<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import {
  settingsApi,
  type EnvironmentName,
  type SiteSettingsDto
} from '@/api/settings';

const { t } = useI18n();

const loading = ref(false);
const saving = ref(false);
const meta = ref<{ updatedBy?: string; updatedAt?: string }>({});

const form = reactive<{
  siteName: string;
  defaultEnvironment: EnvironmentName;
  refreshCron: string;
  maxToolsPerService: number;
  ssoEnabled: boolean;
  auditRetentionDays: number;
  demoMode: boolean;
}>({
  siteName: '',
  defaultEnvironment: 'DEV',
  refreshCron: '',
  maxToolsPerService: 500,
  ssoEnabled: false,
  auditRetentionDays: 90,
  demoMode: true
});

const envOptions: EnvironmentName[] = ['DEV', 'STAGING', 'PROD', 'UNKNOWN'];

function applyDto(dto: SiteSettingsDto) {
  form.siteName = dto.siteName;
  form.defaultEnvironment = dto.defaultEnvironment;
  form.refreshCron = dto.refreshCron;
  form.maxToolsPerService = dto.maxToolsPerService;
  form.ssoEnabled = dto.ssoEnabled;
  form.auditRetentionDays = dto.auditRetentionDays;
  form.demoMode = dto.demoMode;
  meta.value = { updatedBy: dto.updatedBy, updatedAt: dto.updatedAt };
}

async function fetchSettings() {
  loading.value = true;
  try {
    applyDto(await settingsApi.get());
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    loading.value = false;
  }
}

async function submit() {
  saving.value = true;
  try {
    applyDto(await settingsApi.update({ ...form }));
    ElMessage.success(t('settings.saved'));
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    saving.value = false;
  }
}

const updatedAtLabel = computed(() =>
  meta.value.updatedAt ? new Date(meta.value.updatedAt).toLocaleString() : '-'
);

onMounted(fetchSettings);
</script>

<template>
  <div class="mcpg-page" v-loading="loading">
    <header class="header-row">
      <div class="title-block">
        <h2>{{ t('settings.title') }}</h2>
        <p>{{ t('settings.subtitle') }}</p>
      </div>
      <div class="meta-block">
        <span class="meta-label">{{ t('settings.fields.updatedAt') }}:</span>
        <span class="meta-value mono">{{ updatedAtLabel }}</span>
      </div>
    </header>

    <el-card shadow="never" class="form-card">
      <el-form
        label-position="top"
        @submit.prevent="submit"
      >
        <h3 class="section">{{ t('settings.sections.general') }}</h3>
        <div class="grid">
          <el-form-item :label="t('settings.fields.siteName')">
            <el-input v-model="form.siteName" />
          </el-form-item>
          <el-form-item :label="t('settings.fields.defaultEnvironment')">
            <el-select v-model="form.defaultEnvironment" style="width: 100%">
              <el-option v-for="e in envOptions" :key="e" :label="e" :value="e" />
            </el-select>
          </el-form-item>
        </div>

        <h3 class="section">{{ t('settings.sections.schedule') }}</h3>
        <div class="grid">
          <el-form-item :label="t('settings.fields.refreshCron')">
            <el-input v-model="form.refreshCron" class="mono-input" />
            <p class="hint">{{ t('settings.hints.cron') }}</p>
          </el-form-item>
          <el-form-item :label="t('settings.fields.maxToolsPerService')">
            <el-input-number
              v-model="form.maxToolsPerService"
              :min="10"
              :max="10000"
              :step="50"
              style="width: 100%"
            />
            <p class="hint">{{ t('settings.hints.maxTools') }}</p>
          </el-form-item>
        </div>

        <h3 class="section">{{ t('settings.sections.governance') }}</h3>
        <div class="grid">
          <el-form-item :label="t('settings.fields.ssoEnabled')">
            <el-switch v-model="form.ssoEnabled" />
            <p class="hint">{{ t('settings.hints.sso') }}</p>
          </el-form-item>
          <el-form-item :label="t('settings.fields.auditRetentionDays')">
            <el-input-number
              v-model="form.auditRetentionDays"
              :min="1"
              :max="3650"
              :step="30"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <h3 class="section">{{ t('settings.sections.demo') }}</h3>
        <div class="grid">
          <el-form-item :label="t('settings.fields.demoMode')">
            <el-switch v-model="form.demoMode" />
            <p class="hint">{{ t('settings.hints.demo') }}</p>
          </el-form-item>
        </div>

        <div class="actions">
          <el-button type="primary" :loading="saving" @click="submit">
            {{ t('settings.submit') }}
          </el-button>
          <el-button @click="fetchSettings">{{ t('common.refresh') }}</el-button>
        </div>
      </el-form>
    </el-card>
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
.meta-block {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.meta-label {
  margin-right: 6px;
}
.mono,
.mono-input :deep(.el-input__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12.5px;
}
.form-card {
  max-width: 960px;
}
.section {
  margin: 24px 0 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.section:first-of-type {
  margin-top: 0;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
}
@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
.hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  gap: 8px;
}
</style>
