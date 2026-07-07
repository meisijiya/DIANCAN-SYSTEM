<script setup lang="ts">
import { computed, h, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { SimpleScrollbar } from '@sa/materials';
import { GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useRouterPush } from '@/hooks/common/router';
import { useMenu } from '../context';

defineOptions({
  name: 'VerticalMenu'
});

const route = useRoute();
const appStore = useAppStore();
const themeStore = useThemeStore();
const routeStore = useRouteStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const { selectedKey } = useMenu();

const inverted = computed(() => !themeStore.darkMode && themeStore.sider.inverted);

const expandedKeys = ref<string[]>([]);
const displaySelectedKey = ref('');

function createMenuLabelRenderer(menu: App.Global.Menu) {
  return () =>
    h(
      'div',
      {
        class: 'custom-menu-label'
      },
      [
        menu.icon
          ? h(
              'span',
              {
                class: 'custom-menu-label__icon'
              },
              [menu.icon()]
            )
          : null,
        h(
          'span',
          {
            class: 'custom-menu-label__text'
          },
          menu.label
        )
      ]
    );
}

function normalizeMenuOptions(menus: App.Global.Menu[]): any[] {
  return menus.map(menu => ({
    ...menu,
    icon: undefined,
    label: createMenuLabelRenderer(menu),
    children: menu.children ? normalizeMenuOptions(menu.children) : undefined
  }));
}

const menuOptions = computed(() => normalizeMenuOptions(routeStore.menus));

function syncDisplaySelectedKey() {
  displaySelectedKey.value = selectedKey.value;
}

function updateExpandedKeys() {
  if (appStore.siderCollapse || !selectedKey.value) {
    expandedKeys.value = [];
    return;
  }
  expandedKeys.value = routeStore.getSelectedMenuKeyPath(selectedKey.value);
}

async function handleUpdateValue(key: string) {
  displaySelectedKey.value = key;

  try {
    await routerPushByKeyWithMetaQuery(key);
  } catch (error) {
    // 路由跳转失败时回退到当前真实选中菜单，避免高亮状态卡住
    syncDisplaySelectedKey();
  }
}

watch(
  () => route.name,
  () => {
    syncDisplaySelectedKey();
    updateExpandedKeys();
  },
  { immediate: true }
);
</script>

<template>
  <Teleport :to="`#${GLOBAL_SIDER_MENU_ID}`">
    <SimpleScrollbar>
      <NMenu
        v-model:expanded-keys="expandedKeys"
        mode="vertical"
        :value="displaySelectedKey"
        :collapsed="appStore.siderCollapse"
        :collapsed-width="themeStore.sider.collapsedWidth"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :inverted="inverted"
        :indent="18"
        @update:value="handleUpdateValue"
      />
    </SimpleScrollbar>
  </Teleport>
</template>

<style scoped>
:deep(.custom-menu-label) {
  display: flex;
  align-items: center;
  min-width: 0;
  width: 100%;
  white-space: nowrap;
}

:deep(.custom-menu-label__icon) {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  font-size: 18px;
  line-height: 1;
}

:deep(.custom-menu-label__text) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
