<script setup lang="ts">
import { GLOBAL_HEADER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import GlobalLogo from '../global-logo/index.vue';
import GlobalBreadcrumb from '../global-breadcrumb/index.vue';
import GlobalSearch from '../global-search/index.vue';
import UserAvatar from './components/user-avatar.vue';

defineOptions({
  name: 'GlobalHeader'
});

interface Props {
  /** Whether to show the logo */
  showLogo?: App.Global.HeaderProps['showLogo'];
  /** Whether to show the menu toggler */
  showMenuToggler?: App.Global.HeaderProps['showMenuToggler'];
  /** Whether to show the menu */
  showMenu?: App.Global.HeaderProps['showMenu'];
}

defineProps<Props>();

const appStore = useAppStore();
const themeStore = useThemeStore();
</script>

<template>
  <DarkModeContainer class="global-header-shell h-full flex-y-center px-14px shadow-header">
    <GlobalLogo v-if="showLogo" class="h-full" :style="{ width: themeStore.sider.width + 'px' }" />
    <MenuToggler v-if="showMenuToggler" :collapsed="appStore.siderCollapse" @click="appStore.toggleSiderCollapse" />
    <div v-if="showMenu" :id="GLOBAL_HEADER_MENU_ID" class="global-header-menu h-full flex-y-center flex-1-hidden"></div>
    <div v-else class="global-header-main h-full flex-y-center flex-1-hidden">
      <GlobalBreadcrumb v-if="!appStore.isMobile" class="global-header-breadcrumb ml-12px" />
    </div>
    <div class="global-header-actions h-full flex-y-center justify-end">
      <GlobalSearch v-if="themeStore.header.globalSearch.visible" />
      <UserAvatar />
    </div>
  </DarkModeContainer>
</template>

<style scoped>
.global-header-shell {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 24%),
    linear-gradient(180deg, rgba(251, 254, 255, 0.96), rgba(241, 248, 255, 0.98));
  border-bottom: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  box-shadow:
    0 8px 24px rgba(var(--admin-accent-rgb), 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(10px);
}

.global-header-main,
.global-header-menu {
  min-width: 0;
}

.global-header-breadcrumb {
  padding: 8px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.global-header-actions {
  gap: 2px;
}

html.dark .global-header-shell {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.14), transparent 26%),
    linear-gradient(180deg, rgba(7, 10, 16, 0.96), rgba(11, 15, 24, 0.98));
  border-bottom-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 12px 28px rgba(0, 0, 0, 0.36),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .global-header-breadcrumb {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}
</style>
