<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { NConfigProvider, darkTheme } from 'naive-ui';
import type { WatermarkProps } from 'naive-ui';
import { fetchAdminThemePreset } from './service/api/system';
import { getToken } from './store/modules/auth/shared';
import { useAppStore } from './store/modules/app';
import { useThemeStore } from './store/modules/theme';
import { applyThemePreset, getThemePresetById } from './theme/preset/shared';
import { naiveDateLocales, naiveLocales } from './locales/naive';

defineOptions({
  name: 'App'
});

const appStore = useAppStore();
const themeStore = useThemeStore();

const naiveDarkTheme = computed(() => (themeStore.darkMode ? darkTheme : undefined));

const naiveLocale = computed(() => {
  return naiveLocales[appStore.locale];
});

const naiveDateLocale = computed(() => {
  return naiveDateLocales[appStore.locale];
});

const watermarkProps = computed<WatermarkProps>(() => {
  return {
    content: themeStore.watermarkContent,
    cross: true,
    fullscreen: true,
    fontSize: 16,
    lineHeight: 16,
    width: 384,
    height: 384,
    xOffset: 12,
    yOffset: 60,
    rotate: -15,
    zIndex: 9999
  };
});

async function initServerThemePreset() {
  if (!getToken()) {
    return;
  }

  const { data: presetId, error } = await fetchAdminThemePreset();

  if (error || !presetId) {
    return;
  }

  const preset = getThemePresetById(presetId);

  if (!preset) {
    return;
  }

  applyThemePreset(themeStore, preset);
}

onMounted(() => {
  initServerThemePreset();
});
</script>

<template>
  <NConfigProvider
    :theme="naiveDarkTheme"
    :theme-overrides="themeStore.naiveTheme"
    :locale="naiveLocale"
    :date-locale="naiveDateLocale"
    class="h-full"
  >
    <AppProvider>
      <RouterView class="bg-layout" />
      <NWatermark v-if="themeStore.watermark.visible" v-bind="watermarkProps" />
    </AppProvider>
  </NConfigProvider>
</template>

<style scoped></style>
