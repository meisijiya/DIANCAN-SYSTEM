<script setup lang="ts">
import { computed } from 'vue';
import { GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import GlobalLogo from '../global-logo/index.vue';

defineOptions({
  name: 'GlobalSider'
});

const appStore = useAppStore();
const themeStore = useThemeStore();

const isTopHybridSidebarFirst = computed(() => themeStore.layout.mode === 'top-hybrid-sidebar-first');
const isTopHybridHeaderFirst = computed(() => themeStore.layout.mode === 'top-hybrid-header-first');
const darkMenu = computed(
  () =>
    !themeStore.darkMode && !isTopHybridSidebarFirst.value && !isTopHybridHeaderFirst.value && themeStore.sider.inverted
);
const showLogo = computed(() => themeStore.layout.mode === 'vertical');
const menuWrapperClass = computed(() => (showLogo.value ? 'flex-1-hidden' : 'h-full'));
</script>

<template>
  <DarkModeContainer class="global-sider-shell size-full flex-col-stretch shadow-sider" :inverted="darkMenu">
    <GlobalLogo
      v-if="showLogo"
      variant="sider"
      :show-title="!appStore.siderCollapse"
      class="global-sider-logo"
      :style="{ minHeight: themeStore.header.height + 'px' }"
    />
    <div :id="GLOBAL_SIDER_MENU_ID" class="global-sider-menu" :class="menuWrapperClass"></div>
  </DarkModeContainer>
</template>

<style scoped>
.global-sider-shell {
  padding: 12px 10px 12px 12px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--admin-layout-start) 42%, white) 0%, color-mix(in srgb, var(--admin-layout-end) 26%, white) 100%);
  border-right: 1px solid rgba(var(--admin-accent-rgb), 0.08);
}

.global-sider-logo {
  margin: 8px 8px 10px;
  flex-shrink: 0;
}

.global-sider-menu {
  min-height: 0;
  border-radius: 26px;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 24%),
    radial-gradient(circle at bottom right, rgba(var(--admin-accent-rgb), 0.04), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, color-mix(in srgb, var(--admin-layout-start) 34%, white) 100%);
  box-shadow:
    0 18px 30px rgba(var(--admin-accent-rgb), 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  overflow: hidden;
}

html.dark .global-sider-shell {
  background: linear-gradient(180deg, rgba(2, 4, 8, 0.98), rgba(5, 7, 12, 0.98));
  border-right-color: rgba(255, 255, 255, 0.05);
}

html.dark .global-sider-menu {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.14), transparent 25%),
    radial-gradient(circle at bottom right, rgba(var(--admin-accent-rgb), 0.08), transparent 26%),
    linear-gradient(180deg, rgba(8, 12, 20, 0.98), rgba(12, 16, 25, 0.98));
  box-shadow:
    0 22px 36px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.03);
}
</style>
