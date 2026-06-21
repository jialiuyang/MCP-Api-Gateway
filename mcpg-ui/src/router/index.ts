import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

/**
 * Top-level routes for the MCP Gateway console.
 *
 * <p>Routes are organized as <code>/section/[entity]</code> to support deep linking.
 * Each route is lazy-loaded so the initial bundle stays small even as pages grow.</p>
 *
 * <p>{@code meta.titleKey} is an i18n key (resolved against the active locale by
 * the layout); it lets the sidebar / breadcrumbs / browser title stay localized
 * without hard-coding strings in the router.</p>
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'dashboard',
        meta: { titleKey: 'menu.dashboard', icon: 'DataAnalysis' },
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: '/registries',
        name: 'registries',
        meta: { titleKey: 'menu.registries', icon: 'Connection' },
        component: () => import('@/views/Registries.vue')
      },
      {
        path: '/services',
        name: 'services',
        meta: { titleKey: 'menu.services', icon: 'Box' },
        component: () => import('@/views/Services.vue')
      },
      {
        path: '/tools',
        name: 'tools',
        meta: { titleKey: 'menu.tools', icon: 'Tools' },
        component: () => import('@/views/Tools.vue')
      },
      {
        path: '/mcp-connection',
        name: 'mcp-connection',
        meta: { titleKey: 'menu.clients', icon: 'Link' },
        component: () => import('@/views/CursorIntegration.vue')
      },
      {
        path: '/audit',
        name: 'audit',
        meta: { titleKey: 'menu.audit', icon: 'Document' },
        component: () => import('@/views/Audit.vue')
      },
      {
        path: '/policies',
        name: 'policies',
        meta: { titleKey: 'menu.policies', icon: 'Lock' },
        component: () => import('@/views/Policies.vue')
      },
      {
        path: '/health',
        name: 'health',
        meta: { titleKey: 'menu.health', icon: 'TrendCharts' },
        component: () => import('@/views/Health.vue')
      },
      {
        path: '/settings',
        name: 'settings',
        meta: { titleKey: 'menu.settings', icon: 'Setting' },
        component: () => import('@/views/Settings.vue')
      }
    ]
  }
];

export default createRouter({
  history: createWebHistory(),
  routes
});
