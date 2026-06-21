<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const props = defineProps<{
  /** i18n key for the page title (preferred). */
  titleKey?: string;
  /** Pre-translated title fallback (legacy callers). */
  title?: string;
  /** The milestone label, e.g. "B3". */
  milestone?: string;
}>();

const { t, te } = useI18n();

const resolvedTitle = computed(() => {
  if (props.titleKey && te(props.titleKey)) return t(props.titleKey);
  return props.title ?? '';
});

const subTitle = computed(() =>
  props.milestone
    ? t('placeholder.pending', { milestone: props.milestone })
    : t('placeholder.description')
);
</script>

<template>
  <div class="mcpg-page">
    <el-card shadow="hover">
      <el-result icon="info" :title="resolvedTitle" :sub-title="subTitle">
        <template #extra>
          <el-tag size="large" effect="dark">{{ milestone ?? t('common.notImplemented') }}</el-tag>
        </template>
      </el-result>
    </el-card>
  </div>
</template>
