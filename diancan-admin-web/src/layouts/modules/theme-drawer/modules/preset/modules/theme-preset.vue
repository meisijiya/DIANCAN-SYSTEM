<script setup lang="ts">
import { computed } from 'vue';
import { $t } from '@/locales';
import ThemePresetPanel from '@/components/common/theme-preset-panel.vue';
import { getOfficialThemePresets } from '@/theme/preset/shared';

defineOptions({
  name: 'ThemePreset'
});

const presets = computed(() => getOfficialThemePresets());
</script>

<template>
  <ThemePresetPanel
    :presets="presets"
    :title="$t('theme.appearance.preset.title')"
    description="六套门店运营主题会同步作用于 Naive UI 组件、全局主色和后台基础氛围。"
  />
</template>
