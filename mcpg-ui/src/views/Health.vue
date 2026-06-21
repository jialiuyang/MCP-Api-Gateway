<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart, LineChart } from 'echarts/charts';
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components';
import VChart from 'vue-echarts';
import { healthApi, type HealthOverviewDto, type ToolHealthDto } from '@/api/health';

use([CanvasRenderer, BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent]);

const { t } = useI18n();

const loading = ref(false);
const overview = ref<HealthOverviewDto | null>(null);

async function fetchOverview() {
  loading.value = true;
  try {
    overview.value = await healthApi.overview();
  } catch (err) {
    ElMessage.error((err as Error).message);
  } finally {
    loading.value = false;
  }
}

function formatNumber(n: number): string {
  return n.toLocaleString();
}

function formatPercent(rate: number): string {
  return `${(rate * 100).toFixed(2)}%`;
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString();
}

function successRateTag(rate: number): 'success' | 'warning' | 'danger' {
  if (rate >= 0.99) return 'success';
  if (rate >= 0.95) return 'warning';
  return 'danger';
}

const kpis = computed(() => {
  const o = overview.value;
  if (!o) return [];
  return [
    {
      label: t('health.kpis.totalTools'),
      value: formatNumber(o.totalTools),
      accent: '#409eff'
    },
    {
      label: t('health.kpis.activeTools'),
      value: formatNumber(o.activeTools),
      accent: '#67c23a'
    },
    {
      label: t('health.kpis.calls24h'),
      value: formatNumber(o.callsLast24h),
      accent: '#e6a23c'
    },
    {
      label: t('health.kpis.successRate'),
      value: formatPercent(o.globalSuccessRate),
      accent: o.globalSuccessRate >= 0.99 ? '#67c23a' : '#f56c6c'
    },
    {
      label: t('health.kpis.avgLatency'),
      value: `${o.avgLatencyMs} ms`,
      accent: '#909399'
    }
  ];
});

const volumeOption = computed(() => {
  const o = overview.value;
  if (!o) return null;
  return {
    tooltip: { trigger: 'axis' as const },
    legend: {
      data: [
        t('health.charts.callVolumeSuccess'),
        t('health.charts.callVolumeFailure')
      ],
      bottom: 0
    },
    grid: { left: 40, right: 24, top: 24, bottom: 36 },
    xAxis: {
      type: 'category' as const,
      data: o.callVolume24h.map((p) => p.hour),
      axisLabel: { fontSize: 11 }
    },
    yAxis: { type: 'value' as const },
    series: [
      {
        name: t('health.charts.callVolumeSuccess'),
        type: 'line' as const,
        smooth: true,
        areaStyle: { opacity: 0.15 },
        lineStyle: { width: 2 },
        itemStyle: { color: '#67c23a' },
        data: o.callVolume24h.map((p) => p.success)
      },
      {
        name: t('health.charts.callVolumeFailure'),
        type: 'line' as const,
        smooth: true,
        areaStyle: { opacity: 0.15 },
        lineStyle: { width: 2 },
        itemStyle: { color: '#f56c6c' },
        data: o.callVolume24h.map((p) => p.failure)
      }
    ]
  };
});

const latencyOption = computed(() => {
  const o = overview.value;
  if (!o) return null;
  return {
    tooltip: { trigger: 'axis' as const },
    grid: { left: 40, right: 24, top: 24, bottom: 36 },
    xAxis: {
      type: 'category' as const,
      data: o.latencyHistogram.map((b) => b.range)
    },
    yAxis: { type: 'value' as const, name: t('health.charts.latencyAxis') },
    series: [
      {
        type: 'bar' as const,
        data: o.latencyHistogram.map((b) => b.count),
        barWidth: '60%',
        itemStyle: {
          color: '#409eff',
          borderRadius: [6, 6, 0, 0]
        }
      }
    ]
  };
});

const topTools = computed<ToolHealthDto[]>(() => overview.value?.topTools ?? []);

onMounted(fetchOverview);
</script>

<template>
  <div class="mcpg-page" v-loading="loading">
    <header class="header-row">
      <div class="title-block">
        <h2>{{ t('health.title') }}</h2>
        <p>{{ t('health.subtitle') }}</p>
      </div>
      <el-button @click="fetchOverview">{{ t('common.refresh') }}</el-button>
    </header>

    <div class="kpi-row">
      <el-card v-for="k in kpis" :key="k.label" class="kpi-card" shadow="hover">
        <div class="kpi-label">{{ k.label }}</div>
        <div class="kpi-value" :style="{ color: k.accent }">{{ k.value }}</div>
      </el-card>
    </div>

    <div v-if="!overview && !loading" class="empty">
      <el-empty :description="t('health.empty')" />
    </div>

    <template v-else>
      <div class="chart-row">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>{{ t('health.charts.callVolumeTitle') }}</span>
          </template>
          <v-chart
            v-if="volumeOption"
            :option="volumeOption"
            autoresize
            style="height: 280px"
          />
        </el-card>

        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>{{ t('health.charts.latencyTitle') }}</span>
          </template>
          <v-chart
            v-if="latencyOption"
            :option="latencyOption"
            autoresize
            style="height: 280px"
          />
        </el-card>
      </div>

      <el-card shadow="never" class="table-card">
        <template #header>
          <span>{{ t('health.table.title') }}</span>
        </template>
        <el-table :data="topTools" stripe size="default">
          <el-table-column :label="t('health.table.columns.toolName')" min-width="220">
            <template #default="{ row }">
              <span class="mono">{{ (row as ToolHealthDto).toolName }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('health.table.columns.service')" prop="serviceName" min-width="160" />
          <el-table-column :label="t('health.table.columns.calls24h')" width="120" align="right">
            <template #default="{ row }">
              {{ formatNumber((row as ToolHealthDto).callsLast24h) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('health.table.columns.successRate')" width="120">
            <template #default="{ row }">
              <el-tag :type="successRateTag((row as ToolHealthDto).successRate)" size="small" effect="dark">
                {{ formatPercent((row as ToolHealthDto).successRate) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('health.table.columns.p50')" width="80" align="right">
            <template #default="{ row }">{{ (row as ToolHealthDto).p50LatencyMs }}</template>
          </el-table-column>
          <el-table-column :label="t('health.table.columns.p95')" width="80" align="right">
            <template #default="{ row }">{{ (row as ToolHealthDto).p95LatencyMs }}</template>
          </el-table-column>
          <el-table-column :label="t('health.table.columns.p99')" width="80" align="right">
            <template #default="{ row }">{{ (row as ToolHealthDto).p99LatencyMs }}</template>
          </el-table-column>
          <el-table-column :label="t('health.table.columns.lastInvoked')" width="120">
            <template #default="{ row }">
              <span class="mono small">{{ formatTime((row as ToolHealthDto).lastInvokedAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('health.table.columns.lastError')" prop="lastError" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="(row as ToolHealthDto).lastError" class="error-text">
                {{ (row as ToolHealthDto).lastError }}
              </span>
              <span v-else class="ok-text">-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
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
.kpi-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.kpi-card :deep(.el-card__body) {
  padding: 16px 20px;
}
.kpi-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.kpi-value {
  font-size: 28px;
  font-weight: 600;
  margin-top: 6px;
  line-height: 1.1;
}
.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 1280px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
}
.chart-card :deep(.el-card__header) {
  font-weight: 600;
}
.table-card :deep(.el-card__header) {
  font-weight: 600;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12.5px;
}
.mono.small {
  font-size: 11.5px;
}
.error-text {
  color: var(--el-color-danger);
  font-size: 12.5px;
}
.ok-text {
  color: var(--el-text-color-secondary);
}
.empty {
  padding: 80px 0;
  text-align: center;
}
</style>
