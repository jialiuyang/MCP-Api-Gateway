import { createI18n } from 'vue-i18n';
import { ref } from 'vue';
import zhCN from './locales/zh-CN';
import enUS from './locales/en-US';
import elementZh from 'element-plus/es/locale/lang/zh-cn';
import elementEn from 'element-plus/es/locale/lang/en';

/**
 * Locale identifiers. Anything outside this set falls back to the default.
 */
export type LocaleId = 'zh-CN' | 'en-US';

const STORAGE_KEY = 'mcpg.locale';

/**
 * Project default locale.
 *
 * <p>English is the default because this project is open-sourced and the
 * primary documentation lives in English; operators in Chinese teams can
 * flip to {@code zh-CN} via the top-right switcher and the choice is
 * remembered across sessions.</p>
 */
const DEFAULT_LOCALE: LocaleId = 'en-US';

/**
 * Resolve the initial locale from (in order):
 * 1. localStorage (operator's last choice persists across sessions)
 * 2. {@link DEFAULT_LOCALE} - English. We intentionally do NOT auto-pick
 *    based on {@code navigator.language} so first-time visitors see a
 *    predictable English shell regardless of the host browser language;
 *    Chinese operators can switch with one click.
 */
function detectInitialLocale(): LocaleId {
  if (typeof window === 'undefined') return DEFAULT_LOCALE;
  const stored = window.localStorage.getItem(STORAGE_KEY);
  if (stored === 'zh-CN' || stored === 'en-US') return stored;
  return DEFAULT_LOCALE;
}

const initial = detectInitialLocale();

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: initial,
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
});

/**
 * Element Plus locale module bound reactively to the active i18n locale.
 *
 * <p>Components that need ElementPlus-aware locale strings (date pickers,
 * pagination, etc.) bind to {@link elementLocale} via {@code el-config-provider}.</p>
 */
export const elementLocale = ref(initial === 'zh-CN' ? elementZh : elementEn);

/**
 * Switch the global locale. The change is persisted to localStorage so the
 * choice survives page reloads.
 */
export function setLocale(next: LocaleId) {
  i18n.global.locale.value = next;
  elementLocale.value = next === 'zh-CN' ? elementZh : elementEn;
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, next);
    document.documentElement.lang = next;
  }
}

if (typeof document !== 'undefined') {
  document.documentElement.lang = initial;
}

export const SUPPORTED_LOCALES: { id: LocaleId; labelKey: string }[] = [
  { id: 'zh-CN', labelKey: 'language.zh' },
  { id: 'en-US', labelKey: 'language.en' }
];
