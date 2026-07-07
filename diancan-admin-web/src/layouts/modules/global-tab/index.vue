<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useElementBounding } from '@vueuse/core';
import { PageTab } from '@sa/materials';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useTabStore } from '@/store/modules/tab';
import { isPC } from '@/utils/agent';
import BetterScroll from '@/components/custom/better-scroll.vue';
import ContextMenu from './context-menu.vue';

defineOptions({
  name: 'GlobalTab'
});

const route = useRoute();
const appStore = useAppStore();
const themeStore = useThemeStore();
const tabStore = useTabStore();

const bsWrapper = ref<HTMLElement>();
const { width: bsWrapperWidth, left: bsWrapperLeft } = useElementBounding(bsWrapper);
const bsScroll = ref<InstanceType<typeof BetterScroll>>();
const tabRef = ref<HTMLElement>();
const isPCFlag = isPC();
const UNIFIED_TAB_ICON = 'mdi:radiobox-blank';

const TAB_DATA_ID = 'data-tab-id';
const MIDDLE_MOUSE_BUTTON = 1;
const RIGHT_MOUSE_BUTTON = 2;

type TabNamedNodeMap = NamedNodeMap & {
  [TAB_DATA_ID]: Attr;
};

async function scrollToActiveTab() {
  await nextTick();
  if (!tabRef.value) return;

  const { children } = tabRef.value;

  for (let i = 0; i < children.length; i += 1) {
    const child = children[i];

    const { value: tabId } = (child.attributes as TabNamedNodeMap)[TAB_DATA_ID];

    if (tabId === tabStore.activeTabId) {
      const { left, width } = child.getBoundingClientRect();
      const clientX = left + width / 2;

      setTimeout(() => {
        scrollByClientX(clientX);
      }, 50);

      break;
    }
  }
}

function scrollByClientX(clientX: number) {
  const currentX = clientX - bsWrapperLeft.value;
  const deltaX = currentX - bsWrapperWidth.value / 2;

  if (bsScroll.value?.instance) {
    const { maxScrollX, x: leftX, scrollBy } = bsScroll.value.instance;

    const rightX = maxScrollX - leftX;
    const update = deltaX > 0 ? Math.max(-deltaX, rightX) : Math.min(-deltaX, -leftX);

    scrollBy(update, 0, 300);
  }
}

function getContextMenuDisabledKeys(tabId: string) {
  const disabledKeys: App.Global.DropdownKey[] = [];

  if (tabStore.isTabRetain(tabId)) {
    const homeDisable: App.Global.DropdownKey[] = ['closeCurrent', 'closeLeft'];
    disabledKeys.push(...homeDisable);
  }

  return disabledKeys;
}

function handleCloseTab(tab: App.Global.Tab) {
  tabStore.removeTab(tab.id);
}

function handleMousedown(e: MouseEvent, tab: App.Global.Tab) {
  const isMiddleClick = e.button === MIDDLE_MOUSE_BUTTON;
  if (!isMiddleClick || !themeStore.tab.closeTabByMiddleClick) {
    return;
  }

  if (tabStore.isTabRetain(tab.id)) {
    return;
  }

  e.preventDefault();
  handleCloseTab(tab);
}

function switchTab(e: MouseEvent, tab: App.Global.Tab) {
  if ([MIDDLE_MOUSE_BUTTON, RIGHT_MOUSE_BUTTON].includes(e.button)) return;

  tabStore.switchRouteByTab(tab);
}

async function refresh() {
  appStore.reloadPage(500);
}

interface DropdownConfig {
  visible: boolean;
  x: number;
  y: number;
  tabId: string;
}

const dropdown: DropdownConfig = reactive({
  visible: false,
  x: 0,
  y: 0,
  tabId: ''
});

function setDropdown(config: Partial<DropdownConfig>) {
  Object.assign(dropdown, config);
}

let isClickContextMenu = false;

function handleDropdownVisible(visible: boolean | undefined) {
  if (!isClickContextMenu) {
    setDropdown({ visible });
  }
}

async function handleContextMenu(e: MouseEvent, tabId: string) {
  e.preventDefault();

  const { clientX, clientY } = e;

  isClickContextMenu = true;

  const DURATION = dropdown.visible ? 150 : 0;

  setDropdown({ visible: false });

  setTimeout(() => {
    setDropdown({
      visible: true,
      x: clientX,
      y: clientY,
      tabId
    });
    isClickContextMenu = false;
  }, DURATION);
}

function init() {
  tabStore.initTabStore(route);
}

function removeFocus() {
  (document.activeElement as HTMLElement)?.blur();
}

// watch
watch(
  () => route.fullPath,
  () => {
    tabStore.addTab(route);
  }
);
watch(
  () => tabStore.activeTabId,
  () => {
    scrollToActiveTab();
  }
);

// init
init();
</script>

<template>
  <DarkModeContainer class="global-tab-shell size-full flex-y-center px-14px shadow-tab">
    <div ref="bsWrapper" class="h-full flex-1-hidden">
      <BetterScroll ref="bsScroll" :options="{ scrollX: true, scrollY: false, click: !isPCFlag }" @click="removeFocus">
        <div
          ref="tabRef"
          class="h-full flex pr-18px"
          :class="[
            themeStore.tab.mode === 'chrome' || themeStore.tab.mode === 'slider' ? 'items-end' : 'items-center gap-12px'
          ]"
        >
          <PageTab
            v-for="tab in tabStore.tabs"
            :key="tab.id"
            :[TAB_DATA_ID]="tab.id"
            :mode="themeStore.tab.mode"
            :dark-mode="themeStore.darkMode"
            :active="tab.id === tabStore.activeTabId"
            :active-color="themeStore.themeColor"
            :closable="!tabStore.isTabRetain(tab.id)"
            @pointerdown="switchTab($event, tab)"
            @mousedown="handleMousedown($event, tab)"
            @close="handleCloseTab(tab)"
            @contextmenu="handleContextMenu($event, tab.id)"
          >
            <template #prefix>
              <SvgIcon :icon="UNIFIED_TAB_ICON" class="inline-block align-text-bottom text-14px" />
            </template>
            <div class="max-w-240px ellipsis-text">{{ tab.label }}</div>
          </PageTab>
        </div>
      </BetterScroll>
    </div>
    <ReloadButton :loading="!appStore.reloadFlag" @click="refresh" />
    <FullScreen :full="appStore.fullContent" @click="appStore.toggleFullContent" />
  </DarkModeContainer>
  <ContextMenu
    :visible="dropdown.visible"
    :tab-id="dropdown.tabId"
    :disabled-keys="getContextMenuDisabledKeys(dropdown.tabId)"
    :x="dropdown.x"
    :y="dropdown.y"
    @update:visible="handleDropdownVisible"
  />
</template>

<style scoped>
.global-tab-shell {
  background:
    linear-gradient(180deg, rgba(246, 251, 255, 0.98), rgba(235, 244, 255, 0.98));
  border-bottom: 1px solid rgba(var(--admin-accent-rgb), 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.7),
    0 6px 18px rgba(var(--admin-accent-rgb), 0.04);
}

:deep(.sa-page-tab) {
  border-radius: 14px 14px 0 0;
}

:deep(.sa-page-tab:not(.sa-page-tab--active)) {
  color: rgba(21, 44, 76, 0.8);
}

:deep(.sa-page-tab:not(.sa-page-tab--active):hover) {
  background: rgba(var(--admin-accent-rgb), 0.08);
}

:deep(.sa-page-tab--active) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(233, 242, 255, 0.99)) !important;
  color: var(--admin-accent-strong) !important;
  box-shadow:
    0 -1px 0 rgba(255, 255, 255, 0.72),
    0 10px 20px rgba(var(--admin-accent-rgb), 0.12);
}

:deep(.sa-page-tab--active .iconify),
:deep(.sa-page-tab--active .n-base-icon),
:deep(.sa-page-tab--active svg) {
  color: var(--admin-accent-strong) !important;
}

html.dark .global-tab-shell {
  background:
    linear-gradient(180deg, rgba(7, 10, 16, 0.98), rgba(11, 15, 24, 0.98));
  border-bottom-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.03),
    0 10px 22px rgba(0, 0, 0, 0.28);
}

html.dark :deep(.sa-page-tab:not(.sa-page-tab--active)) {
  color: rgba(221, 230, 245, 0.82);
}

html.dark :deep(.sa-page-tab:not(.sa-page-tab--active):hover) {
  background: rgba(255, 255, 255, 0.05);
}

html.dark :deep(.sa-page-tab--active) {
  background:
    linear-gradient(180deg, rgba(20, 27, 41, 0.98), rgba(14, 19, 30, 0.99)) !important;
  color: #f5f8ff !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 12px 22px rgba(0, 0, 0, 0.26);
}

html.dark :deep(.sa-page-tab--active .iconify),
html.dark :deep(.sa-page-tab--active .n-base-icon),
html.dark :deep(.sa-page-tab--active svg) {
  color: #f5f8ff !important;
}
</style>
