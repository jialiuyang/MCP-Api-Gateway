<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { QuestionFilled } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import {
  servicesApi,
  type ServiceDto,
  type ImportSwaggerRequest,
  type UpdateServiceRequest,
  type Environment
} from '@/api/services';

const { t } = useI18n();

const loading = ref(false);
const services = ref<ServiceDto[]>([]);
const filters = reactive<{ keyword: string; environment: Environment | '' }>({
  keyword: '',
  environment: ''
});

const importDialogVisible = ref(false);
const importing = ref(false);
const importForm = reactive<ImportSwaggerRequest>({
  name: '',
  displayName: '',
  url: '',
  baseUrl: '',
  environment: 'DEV'
});

const refreshingId = ref<number | null>(null);

const editDialogVisible = ref(false);
const editing = ref(false);
const editForm = reactive<{ id: number | null } & UpdateServiceRequest>({
  id: null,
  displayName: '',
  baseUrl: '',
  environment: 'DEV'
});

const filtered = computed(() => services.value);

const envOptions: Environment[] = ['DEV', 'STAGING', 'PROD', 'UNKNOWN'];

async function fetchList() {
  loading.value = true;
  try {
    services.value = await servicesApi.list({
      keyword: filters.keyword || undefined,
      environment: filters.environment || undefined
    });
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    loading.value = false;
  }
}

function openImportDialog() {
  importForm.name = '';
  importForm.displayName = '';
  importForm.url = '';
  importForm.baseUrl = '';
  importForm.environment = 'DEV';
  importDialogVisible.value = true;
}

async function submitImport() {
  if (!importForm.name || !importForm.url) {
    ElMessage.warning(t('services.messages.nameUrlRequired'));
    return;
  }
  importing.value = true;
  try {
    const result = await servicesApi.importSwagger({ ...importForm });
    ElMessage.success(
      t('services.messages.imported', {
        name: result.service.name,
        count: result.toolCount,
        added: result.added,
        updated: result.updated,
        removed: result.removed
      })
    );
    importDialogVisible.value = false;
    await fetchList();
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    importing.value = false;
  }
}

async function refreshOne(svc: ServiceDto) {
  refreshingId.value = svc.id;
  try {
    const result = await servicesApi.refresh(svc.id);
    ElMessage.success(
      t('services.messages.refreshed', {
        name: result.service.name,
        added: result.added,
        updated: result.updated,
        removed: result.removed
      })
    );
    await fetchList();
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    refreshingId.value = null;
  }
}

function openEditDialog(svc: ServiceDto) {
  editForm.id = svc.id;
  editForm.displayName = svc.displayName ?? '';
  editForm.baseUrl = svc.baseUrl;
  editForm.environment = svc.environment;
  editDialogVisible.value = true;
}

async function submitEdit() {
  if (editForm.id == null) return;
  if (!editForm.baseUrl || !/^https?:\/\//i.test(editForm.baseUrl)) {
    ElMessage.warning(t('services.messages.baseUrlInvalid'));
    return;
  }
  editing.value = true;
  try {
    await servicesApi.update(editForm.id, {
      displayName: editForm.displayName,
      baseUrl: editForm.baseUrl,
      environment: editForm.environment
    });
    ElMessage.success(t('services.messages.updated'));
    editDialogVisible.value = false;
    await fetchList();
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    editing.value = false;
  }
}

async function deleteOne(svc: ServiceDto) {
  try {
    await ElMessageBox.confirm(
      t('services.deleteConfirm.message', { name: svc.name }),
      t('services.deleteConfirm.title'),
      {
        confirmButtonText: t('services.deleteConfirm.confirmText'),
        cancelButtonText: t('services.deleteConfirm.cancelText'),
        type: 'warning'
      }
    );
    await servicesApi.remove(svc.id);
    ElMessage.success(t('services.messages.deleted', { name: svc.name }));
    await fetchList();
  } catch (err) {
    if (err === 'cancel') return;
    ElMessage.error((err as Error).message);
  }
}

function statusTagType(status: ServiceDto['status']) {
  switch (status) {
    case 'ACTIVE':
      return 'success';
    case 'ERROR':
      return 'danger';
    default:
      return 'info';
  }
}

function formatTimestamp(value?: string) {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

onMounted(fetchList);
</script>

<template>
  <div class="services-page">
    <el-card shadow="never" class="page-header">
      <template #header>
        <div class="header-row">
          <div>
            <h2 class="title">{{ t('services.title') }}</h2>
            <p class="subtitle">{{ t('services.subtitle') }}</p>
          </div>
          <el-button type="primary" :icon="'Plus'" @click="openImportDialog">
            {{ t('services.importButton') }}
          </el-button>
        </div>
      </template>

      <el-form :inline="true" class="filters">
        <el-form-item :label="t('common.search')">
          <el-input
            v-model="filters.keyword"
            :placeholder="t('services.filters.keywordPlaceholder')"
            clearable
            style="width: 240px"
            @keyup.enter="fetchList"
            @clear="fetchList"
          />
        </el-form-item>
        <el-form-item :label="t('services.columns.env')">
          <el-select
            v-model="filters.environment"
            :placeholder="t('services.filters.environmentAll')"
            clearable
            style="width: 160px"
            @change="fetchList"
            @clear="fetchList"
          >
            <el-option v-for="e in envOptions" :key="e" :value="e" :label="e" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :icon="'Search'" @click="fetchList">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="filtered" stripe>
        <el-table-column :label="t('services.columns.name')" prop="name" min-width="160">
          <template #default="{ row }">
            <div>
              <div class="cell-primary">{{ row.name }}</div>
              <div v-if="row.displayName && row.displayName !== row.name" class="cell-secondary">
                {{ row.displayName }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('services.columns.env')" prop="environment" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.environment === 'PROD' ? 'danger' : 'info'">
              {{ row.environment }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('services.columns.source')" prop="sourceType" width="120" />
        <el-table-column :label="t('services.columns.baseUrl')" prop="baseUrl" min-width="240" show-overflow-tooltip />
        <el-table-column :label="t('services.columns.tools')" prop="toolCount" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ row.toolCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('services.columns.status')" prop="status" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">
              {{ t(`services.status.${row.status}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('services.columns.lastSynced')" width="170">
          <template #default="{ row }">{{ formatTimestamp(row.lastSyncedAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row as ServiceDto)">
              {{ t('services.actions.edit') }}
            </el-button>
            <el-button
              size="small"
              :loading="refreshingId === (row as ServiceDto).id"
              @click="refreshOne(row as ServiceDto)"
            >
              {{ t('services.actions.refresh') }}
            </el-button>
            <el-button size="small" type="danger" plain @click="deleteOne(row as ServiceDto)">
              {{ t('services.actions.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && filtered.length === 0"
        :description="t('services.empty.description')"
      >
        <el-button type="primary" @click="openImportDialog">
          {{ t('services.empty.cta') }}
        </el-button>
      </el-empty>
    </el-card>

    <el-dialog
      v-model="editDialogVisible"
      :title="t('services.editDialog.title')"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="120px" label-position="right">
        <el-form-item :label="t('services.editDialog.fields.displayName')">
          <el-input v-model="editForm.displayName" />
        </el-form-item>
        <el-form-item :label="t('services.editDialog.fields.baseUrl')" required>
          <el-input v-model="editForm.baseUrl" placeholder="https://api.example.com" />
          <div class="form-hint">
            <i18n-t keypath="services.editDialog.baseUrlHint">
              <template #http><code>http://</code></template>
              <template #https><code>https://</code></template>
            </i18n-t>
          </div>
        </el-form-item>
        <el-form-item :label="t('services.editDialog.fields.environment')">
          <el-select v-model="editForm.environment">
            <el-option v-for="e in envOptions" :key="e" :value="e" :label="e" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="editing" @click="submitEdit">
          {{ t('services.editDialog.submit') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="importDialogVisible"
      :title="t('services.importDialog.title')"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="importForm" label-width="120px" label-position="right">
        <el-form-item :label="t('services.importDialog.fields.name')" required>
          <el-input v-model="importForm.name" :placeholder="t('services.importDialog.placeholders.name')" />
        </el-form-item>
        <el-form-item :label="t('services.importDialog.fields.displayName')">
          <el-input v-model="importForm.displayName" :placeholder="t('services.importDialog.placeholders.displayName')" />
        </el-form-item>
        <el-form-item required>
          <template #label>
            <span>{{ t('services.importDialog.fields.url') }}</span>
            <el-popover
              trigger="hover"
              placement="top"
              :width="380"
              popper-class="import-help-popover"
            >
              <template #reference>
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </template>
              <div class="import-help">
                <div class="import-help-title">
                  {{ t('services.importDialog.subtitle') }}
                </div>
                <div class="import-help-row">
                  <span class="import-help-label">{{ t('services.importDialog.formatsLabel') }}</span>
                  <span>{{ t('services.importDialog.formats') }}</span>
                </div>
                <div class="import-help-row">
                  <span class="import-help-label">{{ t('services.importDialog.commonPathsLabel') }}</span>
                  <code class="import-help-paths">{{ t('services.importDialog.commonPaths') }}</code>
                </div>
              </div>
            </el-popover>
          </template>
          <el-input v-model="importForm.url" :placeholder="t('services.importDialog.placeholders.url')" />
        </el-form-item>
        <el-form-item :label="t('services.importDialog.fields.baseUrl')">
          <el-input v-model="importForm.baseUrl" :placeholder="t('services.importDialog.placeholders.baseUrl')" />
        </el-form-item>
        <el-form-item :label="t('services.importDialog.fields.environment')">
          <el-select v-model="importForm.environment">
            <el-option v-for="e in envOptions" :key="e" :value="e" :label="e" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">
          {{ t('services.importDialog.submit') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.services-page {
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
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
.filters {
  margin-top: 4px;
}
.table-card :deep(.el-card__body) {
  padding-top: 0;
}
.cell-primary {
  font-weight: 500;
}
.cell-secondary {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.field-help {
  margin-left: 4px;
  color: var(--el-text-color-placeholder);
  cursor: help;
  font-size: 14px;
  vertical-align: middle;
}
.field-help:hover {
  color: var(--el-color-primary);
}
.import-help {
  font-size: 13px;
  line-height: 1.5;
}
.import-help-title {
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}
.import-help-row {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.import-help-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.import-help-paths {
  font-family: var(--el-font-family-monospace, 'SFMono-Regular', Menlo, monospace);
  font-size: 12px;
  word-break: break-all;
  color: var(--el-text-color-regular);
}
.form-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  line-height: 1.5;
}
.form-hint code {
  background: var(--el-fill-color-light);
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
