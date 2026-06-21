<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { auditApi, type AuditEventDto, type AuditOutcome } from '@/api/audit';

const { t } = useI18n();

const loading = ref(false);
const items = ref<AuditEventDto[]>([]);
const total = ref(0);

const filters = reactive<{ keyword: string; outcome: AuditOutcome | '' }>({
  keyword: '',
  outcome: ''
});

const pagination = reactive({ page: 1, size: 20 });

const drawer = reactive<{ visible: boolean; row: AuditEventDto | null }>({
  visible: false,
  row: null
});

async function fetchPage() {
  loading.value = true;
  try {
    const data = await auditApi.list({
      outcome: filters.outcome || undefined,
      keyword: filters.keyword || undefined,
      page: pagination.page - 1,
      size: pagination.size
    });
    items.value = data.items;
    total.value = data.total;
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    loading.value = false;
  }
}

function applyFilters() {
  pagination.page = 1;
  fetchPage();
}

function openDetail(row: AuditEventDto) {
  drawer.row = row;
  drawer.visible = true;
}

function outcomeTag(outcome: AuditOutcome) {
  return outcome === 'SUCCESS' ? 'success' : 'danger';
}

function statusTag(status?: number) {
  if (status == null) return 'info';
  if (status < 300) return 'success';
  if (status < 400) return 'warning';
  return 'danger';
}

const formatDuration = computed(() => (ms?: number) => {
  if (ms == null) return '-';
  if (ms < 1000) return `${ms} ms`;
  return `${(ms / 1000).toFixed(2)} s`;
});

const formatTime = computed(() => (iso: string) => new Date(iso).toLocaleString());

onMounted(fetchPage);
</script>

<template>
  <div class="mcpg-page">
    <header class="header-row">
      <div class="title-block">
        <h2>{{ t('audit.title') }}</h2>
        <p>{{ t('audit.subtitle') }}</p>
      </div>
    </header>

    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent="applyFilters">
        <el-form-item>
          <el-input
            v-model="filters.keyword"
            :placeholder="t('audit.filters.keywordPlaceholder')"
            clearable
            @keyup.enter="applyFilters"
            @clear="applyFilters"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="filters.outcome"
            :placeholder="t('audit.filters.outcomeAll')"
            clearable
            style="width: 160px"
            @change="applyFilters"
          >
            <el-option :label="t('audit.filters.outcomeSuccess')" value="SUCCESS" />
            <el-option :label="t('audit.filters.outcomeFailure')" value="FAILURE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilters">{{ t('common.search') }}</el-button>
          <el-button @click="fetchPage">{{ t('common.refresh') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="items"
        :empty-text="t('audit.empty')"
        stripe
        size="default"
        @row-click="(row: AuditEventDto) => openDetail(row)"
      >
        <el-table-column :label="t('audit.columns.timestamp')" width="180" prop="timestamp">
          <template #default="{ row }">
            <span class="mono">{{ formatTime((row as AuditEventDto).timestamp) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('audit.columns.actor')" prop="actor" width="220">
          <template #default="{ row }">
            <span class="actor">{{ (row as AuditEventDto).actor }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('audit.columns.action')" prop="action" width="180">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ (row as AuditEventDto).action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('audit.columns.resource')" prop="resourceId" width="180">
          <template #default="{ row }">
            <span class="mono">{{ (row as AuditEventDto).resourceType }}/{{ (row as AuditEventDto).resourceId }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('audit.columns.outcome')" width="100">
          <template #default="{ row }">
            <el-tag :type="outcomeTag((row as AuditEventDto).outcome)" effect="dark" size="small">
              {{ t(`audit.outcome.${(row as AuditEventDto).outcome}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('audit.columns.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTag((row as AuditEventDto).httpStatus)" size="small" effect="plain">
              {{ (row as AuditEventDto).httpStatus ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('audit.columns.duration')" width="100">
          <template #default="{ row }">
            <span class="mono">{{ formatDuration((row as AuditEventDto).durationMs) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('audit.columns.detail')" prop="detail" min-width="240" show-overflow-tooltip />
      </el-table>

      <el-pagination
        v-if="total > pagination.size"
        class="pager"
        background
        layout="prev, pager, next, sizes, total"
        :total="total"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="fetchPage"
        @size-change="fetchPage"
      />
    </el-card>

    <el-drawer
      v-model="drawer.visible"
      :title="t('audit.drawer.title')"
      direction="rtl"
      size="480px"
    >
      <div v-if="drawer.row" class="detail">
        <h4>{{ t('audit.drawer.sections.basic') }}</h4>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item :label="t('audit.drawer.labels.id')">{{ drawer.row.id }}</el-descriptions-item>
          <el-descriptions-item :label="t('audit.drawer.labels.timestamp')">
            <span class="mono">{{ formatTime(drawer.row.timestamp) }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('audit.drawer.labels.actor')">{{ drawer.row.actor }}</el-descriptions-item>
          <el-descriptions-item :label="t('audit.drawer.labels.action')">
            <el-tag size="small" type="info" effect="plain">{{ drawer.row.action }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('audit.drawer.labels.resourceType')">{{ drawer.row.resourceType }}</el-descriptions-item>
          <el-descriptions-item :label="t('audit.drawer.labels.resourceId')">
            <span class="mono">{{ drawer.row.resourceId }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('audit.drawer.labels.outcome')">
            <el-tag :type="outcomeTag(drawer.row.outcome)" effect="dark" size="small">
              {{ t(`audit.outcome.${drawer.row.outcome}`) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('audit.drawer.labels.status')">{{ drawer.row.httpStatus ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('audit.drawer.labels.duration')">{{ drawer.row.durationMs ?? '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4>{{ t('audit.drawer.sections.client') }}</h4>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item :label="t('audit.drawer.labels.clientIp')">
            <span class="mono">{{ drawer.row.clientIp ?? '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('audit.drawer.labels.userAgent')">{{ drawer.row.userAgent ?? '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4>{{ t('audit.drawer.sections.detail') }}</h4>
        <p class="detail-text">{{ drawer.row.detail }}</p>
      </div>
    </el-drawer>
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
  margin-bottom: 12px;
}
.table-card :deep(.el-card__body) {
  padding-bottom: 8px;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
.actor {
  font-weight: 500;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
.detail h4 {
  margin: 16px 0 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.detail h4:first-child {
  margin-top: 0;
}
.detail-text {
  margin: 0;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.5;
}
</style>
