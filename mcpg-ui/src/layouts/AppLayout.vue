<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRoute, RouterView } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
  DataAnalysis, Connection, Box, Tools, Link,
  Document, Lock, TrendCharts, Setting, Operation,
  Expand, Fold
} from '@element-plus/icons-vue';
import { setLocale, type LocaleId } from '@/i18n';

const route = useRoute();
const { t, locale } = useI18n();

const iconComponents: Record<string, unknown> = {
  DataAnalysis, Connection, Box, Tools, Link,
  Document, Lock, TrendCharts, Setting, Operation
};

/**
 * Menu structure rendered in the side bar.
 *
 * <p>The visual menu is intentionally decoupled from the router definition so
 * grouping (top-level vs. nested under a parent like "MCP") can evolve
 * independently from the URL hierarchy. Routes themselves stay flat to keep
 * deep links short and predictable.</p>
 */
interface MenuLeaf {
  type: 'item';
  path: string;
  titleKey: string;
  icon: string;
  badge?: string;
}
interface MenuGroup {
  type: 'group';
  /** Stable key used by el-menu to identify the submenu. */
  key: string;
  titleKey: string;
  icon: string;
  children: Omit<MenuLeaf, 'type'>[];
}
type MenuEntry = MenuLeaf | MenuGroup;

const menu: MenuEntry[] = [
  { type: 'item', path: '/dashboard', titleKey: 'menu.dashboard', icon: 'DataAnalysis' },
  { type: 'item', path: '/registries', titleKey: 'menu.registries', icon: 'Connection' },
  { type: 'item', path: '/services', titleKey: 'menu.services', icon: 'Box' },
  {
    type: 'group',
    key: 'group-mcp',
    titleKey: 'menu.mcp',
    icon: 'Operation',
    children: [
      { path: '/tools', titleKey: 'menu.tools', icon: 'Tools' },
      { path: '/mcp-connection', titleKey: 'menu.clients', icon: 'Link' },
      { path: '/audit', titleKey: 'menu.audit', icon: 'Document' },
      { path: '/policies', titleKey: 'menu.policies', icon: 'Lock' },
      { path: '/health', titleKey: 'menu.health', icon: 'TrendCharts' }
    ]
  },
  { type: 'item', path: '/settings', titleKey: 'menu.settings', icon: 'Setting' }
];

const activeMenu = computed(() => route.path);
const collapsed = ref(false);

/**
 * Submenus that should be expanded by default. Whenever the current route
 * lives inside a group, that group's {@code key} is included so the active
 * item is visible without an extra click.
 */
const openedGroups = computed(() => {
  const path = route.path;
  return menu
    .filter((m): m is MenuGroup =>
      m.type === 'group' && m.children.some((c) => c.path === path))
    .map((m) => m.key);
});

const headerTitle = computed(() => {
  const key = route.meta?.titleKey as string | undefined;
  return key ? t(key) : t('app.fullName');
});

function handleLocaleChange(next: string | number | boolean | undefined) {
  if (next === 'zh-CN' || next === 'en-US') {
    setLocale(next as LocaleId);
  }
}
</script>

<template>
  <el-container class="layout-root">
    <el-aside :width="collapsed ? '64px' : '224px'" class="layout-aside">
      <div class="logo">
        <span class="logo-mark">MCP</span>
        <span v-if="!collapsed" class="logo-text">{{ t('app.name') }}</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :default-openeds="openedGroups"
        :collapse="collapsed"
        background-color="#1f2937"
        text-color="#cbd5f5"
        active-text-color="#ffffff"
        router
        class="layout-menu"
      >
        <template v-for="entry in menu" :key="entry.type === 'item' ? entry.path : entry.key">
          <el-menu-item v-if="entry.type === 'item'" :index="entry.path">
            <el-icon v-if="iconComponents[entry.icon]">
              <component :is="iconComponents[entry.icon]" />
            </el-icon>
            <template #title>
              <span>{{ t(entry.titleKey) }}</span>
              <el-tag v-if="entry.badge" size="small" type="warning" effect="dark" class="badge">
                {{ entry.badge }}
              </el-tag>
            </template>
          </el-menu-item>

          <el-sub-menu v-else :index="entry.key">
            <template #title>
              <el-icon v-if="iconComponents[entry.icon]">
                <component :is="iconComponents[entry.icon]" />
              </el-icon>
              <span>{{ t(entry.titleKey) }}</span>
            </template>
            <el-menu-item
              v-for="child in entry.children"
              :key="child.path"
              :index="child.path"
            >
              <el-icon v-if="iconComponents[child.icon]">
                <component :is="iconComponents[child.icon]" />
              </el-icon>
              <template #title>
                <span>{{ t(child.titleKey) }}</span>
                <el-tag v-if="child.badge" size="small" type="warning" effect="dark" class="badge">
                  {{ child.badge }}
                </el-tag>
              </template>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="left">
          <el-button
            text
            :title="collapsed ? t('layout.expandSidebar') : t('layout.collapseSidebar')"
            @click="collapsed = !collapsed"
          >
            <el-icon :size="18">
              <component :is="collapsed ? Expand : Fold" />
            </el-icon>
          </el-button>
          <span class="header-title">{{ headerTitle }}</span>
        </div>
        <div class="right">
          <el-radio-group
            :model-value="locale"
            size="small"
            class="lang-switcher"
            @change="handleLocaleChange"
          >
            <el-radio-button value="zh-CN" label="zh-CN">中文</el-radio-button>
            <el-radio-button value="en-US" label="en-US">EN</el-radio-button>
          </el-radio-group>
          <el-tag type="success" effect="plain" class="live-tag">
            <span class="live-dot" /> {{ t('layout.live') }}
          </el-tag>
        </div>
      </el-header>
      <el-main class="layout-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-root { height: 100vh; }

.layout-aside {
  background: var(--mcpg-side);
  color: #cbd5f5;
  transition: width 0.2s ease;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-mark {
  background: linear-gradient(135deg, #409eff, #7b61ff);
  color: #fff;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 6px;
  letter-spacing: 0.5px;
}
.logo-text { font-weight: 600; color: #f1f5f9; font-size: 16px; }

.layout-menu { border-right: none; }
.layout-menu :deep(.el-menu-item) {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}
.layout-menu :deep(.el-menu-item.is-active) {
  background-color: var(--mcpg-side-active);
}
.layout-menu :deep(.el-sub-menu__title) {
  color: #cbd5f5;
}
.layout-menu :deep(.el-sub-menu .el-menu) {
  background-color: rgba(0, 0, 0, 0.15) !important;
}
.layout-menu :deep(.el-sub-menu .el-menu-item) {
  background-color: transparent !important;
}
.layout-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background-color: var(--mcpg-side-active) !important;
}
.badge { margin-left: 8px; }

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--mcpg-border);
  padding: 0 24px;
}
.left { display: flex; align-items: center; gap: 12px; }
.header-title { font-size: 16px; font-weight: 600; color: var(--mcpg-text); }
.right { display: flex; align-items: center; gap: 12px; }
.lang-switcher :deep(.el-radio-button__inner) {
  padding: 6px 14px;
  font-weight: 500;
}
.live-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-success);
  box-shadow: 0 0 0 0 var(--el-color-success);
  animation: live-pulse 1.6s ease-out infinite;
}
@keyframes live-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.55);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(103, 194, 58, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
  }
}

.layout-main {
  background: var(--mcpg-bg);
  padding: 0;
  overflow: auto;
}
</style>
