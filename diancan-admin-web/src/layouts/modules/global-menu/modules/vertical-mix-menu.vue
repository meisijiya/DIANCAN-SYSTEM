<script setup lang="ts">
import { computed, h, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { SimpleScrollbar } from '@sa/materials';
import { useBoolean } from '@sa/hooks';
import type { RouteKey } from '@elegant-router/types';
import { GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useRouterPush } from '@/hooks/common/router';
import { $t } from '@/locales';
import { useMenu, useMixMenuContext } from '../context';
import FirstLevelMenu from '../components/first-level-menu.vue';
import GlobalLogo from '../../global-logo/index.vue';

defineOptions({
  name: 'VerticalMixMenu'
});

const route = useRoute();
const appStore = useAppStore();
const themeStore = useThemeStore();
const routeStore = useRouteStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const { bool: drawerVisible, setBool: setDrawerVisible } = useBoolean();
const {
  firstLevelMenus,
  secondLevelMenus,
  activeFirstLevelMenuKey,
  isActiveFirstLevelMenuHasChildren,
  getActiveFirstLevelMenuKey,
  handleSelectFirstLevelMenu
} = useMixMenuContext('VerticalMixMenu');
const { selectedKey } = useMenu();

const inverted = computed(() => !themeStore.darkMode && themeStore.sider.inverted);

const hasChildMenus = computed(() => secondLevelMenus.value.length > 0);

const showDrawer = computed(() => hasChildMenus.value && (drawerVisible.value || appStore.mixSiderFixed));
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

const drawerMenuOptions = computed(() => normalizeMenuOptions(secondLevelMenus.value));

function syncDisplaySelectedKey() {
  displaySelectedKey.value = selectedKey.value;
}

function handleSelectMenu(key: RouteKey) {
  handleSelectFirstLevelMenu(key);

  if (isActiveFirstLevelMenuHasChildren.value) {
    setDrawerVisible(true);
  }
}

function handleResetActiveMenu() {
  setDrawerVisible(false);

  if (!appStore.mixSiderFixed) {
    getActiveFirstLevelMenuKey();
  }
}

const expandedKeys = ref<string[]>([]);

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
    // 路由跳转失败时恢复为真实路由对应菜单
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
    <div class="h-full flex" @mouseleave="handleResetActiveMenu">
      <FirstLevelMenu
        :menus="firstLevelMenus"
        :active-menu-key="activeFirstLevelMenuKey"
        :inverted="inverted"
        :sider-collapse="appStore.siderCollapse"
        :dark-mode="themeStore.darkMode"
        :theme-color="themeStore.themeColor"
        @select="handleSelectMenu"
        @toggle-sider-collapse="appStore.toggleSiderCollapse"
      >
        <GlobalLogo :show-title="false" :style="{ height: themeStore.header.height + 'px' }" />
      </FirstLevelMenu>
      <div
        class="relative h-full transition-width-300"
        :style="{ width: appStore.mixSiderFixed && hasChildMenus ? themeStore.sider.mixChildMenuWidth + 'px' : '0px' }"
      >
        <DarkModeContainer
          class="vertical-mix-drawer absolute-lt h-full flex-col-stretch nowrap-hidden transition-all-300"
          :inverted="inverted"
          :style="{ width: showDrawer ? themeStore.sider.mixChildMenuWidth + 'px' : '0px' }"
        >
          <header class="vertical-mix-drawer__header flex-y-center justify-between px-14px" :style="{ height: themeStore.header.height + 'px' }">
            <div>
              <div class="vertical-mix-drawer__eyebrow">SECTION</div>
              <h2 class="text-16px text-primary font-bold">{{ $t('system.title') }}</h2>
            </div>
            <PinToggler
              :pin="appStore.mixSiderFixed"
              :class="{ 'text-white:88 !hover:text-white': inverted }"
              @click="appStore.toggleMixSiderFixed"
            />
          </header>
          <SimpleScrollbar>
            <NMenu
              v-model:expanded-keys="expandedKeys"
              mode="vertical"
              :value="displaySelectedKey"
              :options="drawerMenuOptions"
              :inverted="inverted"
              :indent="18"
              @update:value="handleUpdateValue"
            />
          </SimpleScrollbar>
        </DarkModeContainer>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.vertical-mix-drawer {
  border-left: 1px solid rgba(var(--admin-accent-rgb), 0.08);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.98));
  box-shadow:
    8px 0 22px rgba(var(--admin-accent-rgb), 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.vertical-mix-drawer__header {
  border-bottom: 1px solid rgba(var(--admin-accent-rgb), 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.vertical-mix-drawer__eyebrow {
  margin-bottom: 4px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: rgba(var(--admin-accent-rgb), 0.56);
}

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
  margin-right: 8px;
  font-size: 16px;
  line-height: 1;
}

:deep(.custom-menu-label__text) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

html.dark .vertical-mix-drawer {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 24%),
    linear-gradient(180deg, rgba(8, 12, 20, 0.98), rgba(12, 16, 25, 0.98));
  border-left-color: rgba(255, 255, 255, 0.05);
  box-shadow:
    8px 0 24px rgba(0, 0, 0, 0.36),
    inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

html.dark .vertical-mix-drawer__header {
  background: rgba(255, 255, 255, 0.03);
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

html.dark .vertical-mix-drawer__eyebrow {
  color: rgba(183, 198, 228, 0.62);
}
</style>
