<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import {
  registriesApi,
  type RegistryDto,
  type RegistryTypeDto,
  type CreateRegistryRequest,
  type UpdateRegistryRequest,
  type DiscoveryResultDto
} from '@/api/registries';
import type { Environment } from '@/api/services';

const { t } = useI18n();

const loading = ref(false);
const registries = ref<RegistryDto[]>([]);
const types = ref<RegistryTypeDto[]>([]);

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const dialogSubmitting = ref(false);
const editingId = ref<number | null>(null);
const form = reactive<CreateRegistryRequest>({
  name: '',
  displayName: '',
  type: 'nacos',
  endpoint: '',
  username: '',
  password: '',
  environment: 'DEV',
  namespace: '',
  groupName: '',
  extra: '',
  enabled: true
});

const discoveryDialogVisible = ref(false);
const discoveryResult = ref<DiscoveryResultDto | null>(null);
const actionId = ref<number | null>(null);
const actionKind = ref<'test' | 'discover' | null>(null);

const environments: Environment[] = ['DEV', 'STAGING', 'PROD', 'UNKNOWN'];

const implementedTypes = computed(() => types.value.filter((t) => t.implemented));
const previewTypes = computed(() => types.value.filter((t) => !t.implemented));

async function fetchAll() {
  loading.value = true;
  try {
    const [list, ts] = await Promise.all([
      registriesApi.list(),
      registriesApi.types()
    ]);
    registries.value = list;
    types.value = ts;
  } catch (e) {
    ElMessage.error((e as Error).message);
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  dialogMode.value = 'create';
  editingId.value = null;
  Object.assign(form, {
    name: '',
    displayName: '',
    type: implementedTypes.value[0]?.type ?? 'nacos',
    endpoint: '',
    username: '',
    password: '',
    environment: 'DEV',
    namespace: '',
    groupName: '',
    extra: '',
    enabled: true
  });
  dialogVisible.value = true;
}

function openEditDialog(row: RegistryDto) {
  dialogMode.value = 'edit';
  editingId.value = row.id;
  Object.assign(form, {
    name: row.name,
    displayName: row.displayName ?? '',
    type: row.type,
    endpoint: row.endpoint,
    username: row.username ?? '',
    password: '',
    environment: row.environment,
    namespace: row.namespace ?? '',
    groupName: row.groupName ?? '',
    extra: row.extra ?? '',
    enabled: row.enabled
  });
  dialogVisible.value = true;
}

async function submitForm() {
  if (!form.name?.trim()) {
    ElMessage.warning(t('registries.messages.nameRequired'));
    return;
  }
  if (!form.endpoint?.trim()) {
    ElMessage.warning(t('registries.messages.endpointRequired'));
    return;
  }
  dialogSubmitting.value = true;
  try {
    if (dialogMode.value === 'create') {
      const created = await registriesApi.create({ ...form });
      ElMessage.success(t('registries.messages.created', { name: created.name }));
    } else if (editingId.value != null) {
      const update: UpdateRegistryRequest = {
        displayName: form.displayName,
        endpoint: form.endpoint,
        username: form.username,
        // null means "no change"; we explicitly send undefined when the
        // user did not type anything new. Empty string would clear the
        // stored password.
        password: form.password === '' ? undefined : form.password,
        environment: form.environment,
        namespace: form.namespace,
        groupName: form.groupName,
        extra: form.extra,
        enabled: form.enabled
      };
      await registriesApi.update(editingId.value, update);
      ElMessage.success(t('registries.messages.updated'));
    }
    dialogVisible.value = false;
    await fetchAll();
  } catch (e) {
    ElMessage.error((e as Error).message);
  } finally {
    dialogSubmitting.value = false;
  }
}

async function testOne(row: RegistryDto) {
  actionId.value = row.id;
  actionKind.value = 'test';
  try {
    const result = await registriesApi.test(row.id);
    if (result.ok) {
      ElMessage.success(t('registries.messages.testOk', { ms: result.elapsedMs }));
    } else {
      ElMessage.error(t('registries.messages.testFail', { reason: result.message ?? '-' }));
    }
    await fetchAll();
  } catch (e) {
    ElMessage.error((e as Error).message);
  } finally {
    actionId.value = null;
    actionKind.value = null;
  }
}

async function discoverOne(row: RegistryDto) {
  actionId.value = row.id;
  actionKind.value = 'discover';
  try {
    const result = await registriesApi.discover(row.id);
    discoveryResult.value = result;
    discoveryDialogVisible.value = true;
    ElMessage.success(
      t('registries.messages.discoveryOk', {
        name: row.name,
        discovered: result.discovered,
        imported: result.imported,
        skipped: result.skipped
      })
    );
    await fetchAll();
  } catch (e) {
    ElMessage.error((e as Error).message);
  } finally {
    actionId.value = null;
    actionKind.value = null;
  }
}

async function toggleEnabled(row: RegistryDto, next: boolean) {
  try {
    await registriesApi.update(row.id, { enabled: next });
    row.enabled = next;
  } catch (e) {
    ElMessage.error((e as Error).message);
    await fetchAll();
  }
}

async function deleteOne(row: RegistryDto) {
  try {
    await ElMessageBox.confirm(
      t('registries.deleteConfirm.message', { name: row.name }),
      t('registries.deleteConfirm.title'),
      {
        confirmButtonText: t('registries.deleteConfirm.confirmText'),
        cancelButtonText: t('registries.deleteConfirm.cancelText'),
        type: 'warning'
      }
    );
    await registriesApi.remove(row.id);
    ElMessage.success(t('registries.messages.deleted', { name: row.name }));
    await fetchAll();
  } catch (err) {
    if (err === 'cancel') return;
    ElMessage.error((err as Error).message);
  }
}

function statusTagType(status: RegistryDto['status']) {
  switch (status) {
    case 'OK':
      return 'success';
    case 'ERROR':
      return 'danger';
    default:
      return 'info';
  }
}

function outcomeTagType(s: string) {
  switch (s) {
    case 'IMPORTED':
    case 'UPDATED':
      return 'success';
    case 'SKIPPED':
      return 'warning';
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

onMounted(fetchAll);
</script>

<template>
  <div class="registries-page">
    <el-card shadow="never" class="page-header">
      <template #header>
        <div class="header-row">
          <div>
            <h2 class="title">{{ t('registries.title') }}</h2>
            <p class="subtitle">{{ t('registries.subtitle') }}</p>
          </div>
          <el-button type="primary" :icon="'Plus'" @click="openCreateDialog">
            {{ t('registries.addButton') }}
          </el-button>
        </div>
      </template>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="registries" stripe>
        <el-table-column :label="t('registries.columns.name')" prop="name" min-width="160">
          <template #default="{ row }">
            <div>
              <div class="cell-primary">{{ (row as RegistryDto).name }}</div>
              <div
                v-if="(row as RegistryDto).displayName && (row as RegistryDto).displayName !== (row as RegistryDto).name"
                class="cell-secondary"
              >
                {{ (row as RegistryDto).displayName }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('registries.columns.type')" width="110">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ (row as RegistryDto).type.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('registries.columns.endpoint')" prop="endpoint" min-width="220" show-overflow-tooltip />
        <el-table-column :label="t('registries.columns.env')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="(row as RegistryDto).environment === 'PROD' ? 'danger' : 'info'">
              {{ (row as RegistryDto).environment }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('registries.columns.services')" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="success">
              {{ (row as RegistryDto).lastServiceCount ?? '—' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('registries.columns.status')" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType((row as RegistryDto).status)">
              {{ t(`registries.status.${(row as RegistryDto).status}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('registries.columns.lastSynced')" width="170">
          <template #default="{ row }">{{ formatTimestamp((row as RegistryDto).lastSyncedAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('registries.columns.enabled')" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="(row as RegistryDto).enabled"
              @click.stop
              @change="(v: boolean | string | number) => toggleEnabled(row as RegistryDto, Boolean(v))"
            />
          </template>
        </el-table-column>
        <el-table-column label="" width="340" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :loading="actionId === (row as RegistryDto).id && actionKind === 'test'"
              @click="testOne(row as RegistryDto)"
            >
              {{ t('registries.actions.test') }}
            </el-button>
            <el-button
              size="small"
              type="primary"
              plain
              :loading="actionId === (row as RegistryDto).id && actionKind === 'discover'"
              @click="discoverOne(row as RegistryDto)"
            >
              {{ t('registries.actions.discover') }}
            </el-button>
            <el-button size="small" @click="openEditDialog(row as RegistryDto)">
              {{ t('registries.actions.edit') }}
            </el-button>
            <el-button size="small" type="danger" plain @click="deleteOne(row as RegistryDto)">
              {{ t('registries.actions.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && registries.length === 0" :description="t('registries.empty.description')">
        <el-button type="primary" @click="openCreateDialog">
          {{ t('registries.empty.cta') }}
        </el-button>
      </el-empty>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('registries.dialog.createTitle') : t('registries.dialog.editTitle')"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="140px" label-position="right">
        <el-form-item :label="t('registries.dialog.fields.name')" required>
          <el-input v-model="form.name" :disabled="dialogMode === 'edit'" :placeholder="t('registries.dialog.placeholders.name')" />
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.displayName')">
          <el-input v-model="form.displayName" :placeholder="t('registries.dialog.placeholders.displayName')" />
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.type')" required>
          <el-select v-model="form.type" :disabled="dialogMode === 'edit'" style="width: 100%">
            <el-option-group v-if="implementedTypes.length" label="">
              <el-option
                v-for="opt in implementedTypes"
                :key="opt.type"
                :value="opt.type"
                :label="opt.label"
              />
            </el-option-group>
            <el-option-group v-if="previewTypes.length" :label="t('registries.typeBadge.preview')">
              <el-option
                v-for="opt in previewTypes"
                :key="opt.type"
                :value="opt.type"
                :label="opt.label"
                :disabled="true"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.endpoint')" required>
          <el-input v-model="form.endpoint" :placeholder="t('registries.dialog.placeholders.endpoint')" />
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.username')">
          <el-input v-model="form.username" :placeholder="t('registries.dialog.placeholders.username')" />
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.password')">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="t('registries.dialog.placeholders.password')"
          />
          <div v-if="dialogMode === 'edit'" class="form-hint">
            {{ t('registries.dialog.hints.passwordHint') }}
          </div>
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.environment')">
          <el-select v-model="form.environment" style="width: 200px">
            <el-option v-for="e in environments" :key="e" :value="e" :label="e" />
          </el-select>
          <div class="form-hint">{{ t('registries.dialog.hints.envHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.namespace')">
          <el-input v-model="form.namespace" :placeholder="t('registries.dialog.placeholders.namespace')" />
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.groupName')">
          <el-input v-model="form.groupName" :placeholder="t('registries.dialog.placeholders.groupName')" />
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.extra')">
          <el-input v-model="form.extra" :placeholder="t('registries.dialog.placeholders.extra')" />
        </el-form-item>
        <el-form-item :label="t('registries.dialog.fields.enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitForm">
          {{ t('registries.dialog.submit') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="discoveryDialogVisible"
      :title="t('registries.discoveryDialog.title', { name: discoveryResult?.registryName ?? '' })"
      width="720px"
    >
      <div v-if="discoveryResult" class="discovery-body">
        <el-alert
          type="info"
          :closable="false"
          :title="t('registries.discoveryDialog.summary', {
            discovered: discoveryResult.discovered,
            imported: discoveryResult.imported,
            skipped: discoveryResult.skipped
          })"
        />
        <el-table :data="discoveryResult.items" stripe max-height="420">
          <el-table-column :label="t('registries.discoveryDialog.columns.name')" prop="name" min-width="160" />
          <el-table-column :label="t('registries.discoveryDialog.columns.baseUrl')" prop="baseUrl" min-width="240" show-overflow-tooltip />
          <el-table-column :label="t('registries.discoveryDialog.columns.status')" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="outcomeTagType((row as any).status)">
                {{ t(`registries.discoveryDialog.itemStatus.${(row as any).status}`) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('registries.discoveryDialog.columns.message')" prop="message" min-width="220" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="discoveryDialogVisible = false">
          {{ t('registries.discoveryDialog.close') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.registries-page {
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
.cell-primary { font-weight: 500; }
.cell-secondary {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.form-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.discovery-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
