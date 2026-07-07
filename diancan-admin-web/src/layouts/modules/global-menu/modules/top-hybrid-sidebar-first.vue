<script setup lang="ts">
import { ref, watch } from 'vue';
import type { RouteKey } from '@elegant-router/types';
import { useRoute } from 'vue-router';
import { GLOBAL_HEADER_MENU_ID, GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouterPush } from '@/hooks/common/router';
import FirstLevelMenu from '../components/first-level-menu.vue';
import { useMenu, useMixMenuContext } from '../context';

defineOptions({
  name: 'TopHybridSidebarFirst'
});

const route = useRoute();
const appStore = useAppStore();
const themeStore = useThemeStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const {
  firstLevelMenus,
  secondLevelMenus,
  activeFirstLevelMenuKey,
  handleSelectFirstLevelMenu,
  activeDeepestLevelMenuKey
} = useMixMenuContext('TopHybridSidebarFirst');
const { selectedKey } = useMenu();
const displaySelectedKey = ref('');

function syncDisplaySelectedKey() {
  displaySelectedKey.value = selectedKey.value;
}

/**
 * Handle first level menu select
 * @param key RouteKey
 */
function handleSelectMenu(key: RouteKey) {
  handleSelectFirstLevelMenu(key);

  // if there are second level menus, select the deepest one by default
  activeDeepestLevelMenuKey();
}

async function handleUpdateValue(key: string) {
  displaySelectedKey.value = key;

  try {
    await routerPushByKeyWithMetaQuery(key);
  } catch (error) {
    // 路由跳转失败时恢复到当前真实选中菜单
    syncDisplaySelectedKey();
  }
}

watch(
  () => route.name,
  () => {
    syncDisplaySelectedKey();
  },
  { immediate: true }
);
</script>

<template>
  <Teleport :to="`#${GLOBAL_HEADER_MENU_ID}`">
    <NMenu
      mode="horizontal"
      :value="displaySelectedKey"
      :options="secondLevelMenus"
      :indent="18"
      responsive
      @update:value="handleUpdateValue"
    />
  </Teleport>
  <Teleport :to="`#${GLOBAL_SIDER_MENU_ID}`">
    <div class="h-full pt-2">
      <FirstLevelMenu
        :menus="firstLevelMenus"
        :active-menu-key="activeFirstLevelMenuKey"
        :sider-collapse="appStore.siderCollapse"
        :dark-mode="themeStore.darkMode"
        :theme-color="themeStore.themeColor"
        @select="handleSelectMenu"
        @toggle-sider-collapse="appStore.toggleSiderCollapse"
      />
    </div>
  </Teleport>
</template>

<style scoped></style>
