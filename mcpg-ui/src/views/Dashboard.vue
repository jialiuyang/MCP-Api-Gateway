<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { systemApi, type SystemInfo } from '@/api';
import { servicesApi, type ServiceDto } from '@/api/services';
import { toolsApi, type ToolDto } from '@/api/tools';
import { registriesApi, type RegistryDto } from '@/api/registries';
import { healthApi, type HealthOverviewDto } from '@/api/health';

const { t } = useI18n();

const info = ref<SystemInfo | null>(null);
const loadError = ref<string>('');
const services = ref<ServiceDto[]>([]);
const tools = ref<ToolDto[]>([]);
const registries = ref<RegistryDto[]>([]);
const health = ref<HealthOverviewDto | null>(null);
const loading = ref(false);

const stats = computed(() => [
  {
    label: t('dashboard.cards.registries'),
    value: registries.value.length.toString(),
    sub: registries.value.length === 0
      ? t('dashboard.cardSub.registriesEmpty')
      : t('dashboard.cardSub.registriesActive', {
          enabled: registries.value.filter((r) => r.enabled).length
        }),
    accent: '#409eff'
  },
  {
    label: t('dashboard.cards.services'),
    value: services.value.length.toString(),
    sub: services.value.length === 0
      ? t('dashboard.cardSub.servicesEmpty')
      : t('dashboard.cardSub.servicesActive'),
    accent: '#67c23a'
  },
  {
    label: t('dashboard.cards.tools'),
    value: tools.value.length.toString(),
    sub: t('dashboard.cardSub.toolsPromoted', {
      count: tools.value.filter((t) => t.promoted).length
    }),
    accent: '#e6a23c'
  },
  {
    label: t('dashboard.cards.calls24h'),
    value: health.value ? health.value.callsLast24h.toLocaleString() : '—',
    sub: health.value
      ? t('dashboard.cardSub.callsLive', {
          rate: (health.value.globalSuccessRate * 100).toFixed(1)
        })
      : t('dashboard.cardSub.callsPending'),
    accent: '#f56c6c'
  }
]);

onMounted(async () => {
  loading.value = true;
  try {
    const [sysInfo, svc, tls, regs, hp] = await Promise.all([
      systemApi.info().catch((e) => {
        loadError.value = (e as Error).message;
        return null;
      }),
      servicesApi.list().catch(() => [] as ServiceDto[]),
      toolsApi.list().catch(() => [] as ToolDto[]),
      registriesApi.list().catch(() => [] as RegistryDto[]),
      healthApi.overview().catch(() => null as HealthOverviewDto | null)
    ]);
    info.value = sysInfo;
    services.value = svc ?? [];
    tools.value = tls ?? [];
    registries.value = regs ?? [];
    health.value = hp;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="mcpg-page">
    <el-row :gutter="16">
      <el-col v-for="s in stats" :key="s.label" :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value" :style="{ color: s.accent }">{{ s.value }}</div>
          <div class="stat-sub">{{ s.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="mcpg-card-title">{{ t('dashboard.welcomeTitle') }}</div>
          </template>
          <i18n-t keypath="dashboard.welcomeBody" tag="p">
            <template #servicesLink>
              <router-link to="/services">{{ t('menu.services') }}</router-link>
            </template>
            <template #toolsLink>
              <router-link to="/tools">{{ t('menu.tools') }}</router-link>
            </template>
            <template #cursorLink>
              <router-link to="/mcp-connection">{{ t('menu.clients') }}</router-link>
            </template>
          </i18n-t>

          <el-divider />

          <div v-if="info">
            <p><strong>{{ t('dashboard.appInfo.application') }}：</strong> {{ info.name }}</p>
            <p><strong>{{ t('dashboard.appInfo.version') }}：</strong> {{ info.version }}</p>
            <p><strong>{{ t('dashboard.appInfo.timestamp') }}：</strong> {{ info.timestamp }}</p>
            <p><strong>{{ t('dashboard.appInfo.status') }}：</strong>
              <el-tag :type="info.status === 'UP' ? 'success' : 'danger'">{{ info.status }}</el-tag>
            </p>
          </div>
          <el-alert v-else-if="loadError" :title="loadError" type="error" show-icon :closable="false" />
          <el-skeleton v-else :rows="3" animated />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="mcpg-card-title">{{ t('dashboard.roadmap') }}</div>
          </template>
          <el-timeline>
            <el-timeline-item type="success" timestamp="B1">
              {{ t('dashboard.roadmapItems.b1') }}
            </el-timeline-item>
            <el-timeline-item type="success" timestamp="B2">
              {{ t('dashboard.roadmapItems.b2') }}
            </el-timeline-item>
            <el-timeline-item type="success" timestamp="B3">
              {{ t('dashboard.roadmapItems.b3') }}
            </el-timeline-item>
            <el-timeline-item type="success" timestamp="B4">
              {{ t('dashboard.roadmapItems.b4') }}
            </el-timeline-item>
            <el-timeline-item type="success" timestamp="B5">
              {{ t('dashboard.roadmapItems.b5') }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-card { border-radius: 8px; }
.stat-label { color: var(--mcpg-text-light); font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; line-height: 1; }
.stat-sub { color: var(--mcpg-text-light); font-size: 12px; margin-top: 8px; }
</style>
