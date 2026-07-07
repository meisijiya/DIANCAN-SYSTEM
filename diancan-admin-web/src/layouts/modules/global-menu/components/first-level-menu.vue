<script setup lang="ts">
import { computed } from 'vue';
import { createReusableTemplate } from '@vueuse/core';
import { SimpleScrollbar } from '@sa/materials';
import { transformColorWithOpacity } from '@sa/color';
import type { RouteKey } from '@elegant-router/types';

defineOptions({
  name: 'FirstLevelMenu'
});

interface Props {
  menus: App.Global.Menu[];
  activeMenuKey?: string;
  inverted?: boolean;
  siderCollapse?: boolean;
  darkMode?: boolean;
  themeColor: string;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'select', menuKey: RouteKey): boolean;
  (e: 'toggleSiderCollapse'): void;
}

const emit = defineEmits<Emits>();

interface MixMenuItemProps {
  /** Menu item label */
  label: App.Global.Menu['label'];
  /** Menu item icon */
  icon: App.Global.Menu['icon'];
  /** Active menu item */
  active: boolean;
  /** Mini size */
  isMini?: boolean;
}
const [DefineMixMenuItem, MixMenuItem] = createReusableTemplate<MixMenuItemProps>();

const selectedBgColor = computed(() => {
  const { darkMode, themeColor } = props;

  const light = transformColorWithOpacity(themeColor, 0.1, '#ffffff');
  const dark = transformColorWithOpacity(themeColor, 0.3, '#000000');

  return darkMode ? dark : light;
});

const hoverBgColor = computed(() => {
  const { darkMode, themeColor } = props;

  const light = transformColorWithOpacity(themeColor, 0.06, '#ffffff');
  const dark = transformColorWithOpacity(themeColor, 0.16, '#000000');

  return darkMode ? dark : light;
});

const hoverTextColor = computed(() => (props.inverted ? 'rgba(255,255,255,0.92)' : 'rgb(var(--base-text-color))'));

function handleClickMixMenu(menuKey: RouteKey) {
  emit('select', menuKey);
}

function toggleSiderCollapse() {
  emit('toggleSiderCollapse');
}
</script>

<template>
  <!-- define component: MixMenuItem -->
  <DefineMixMenuItem v-slot="{ label, icon, active, isMini }">
    <div
      class="mix-menu-item mx-4px mb-8px flex-col-center cursor-pointer rounded-14px bg-transparent px-6px py-10px"
      :class="{
        'text-primary selected-mix-menu': active,
        'text-white:65': inverted,
        '!text-white !bg-primary': active && inverted
      }"
    >
      <component :is="icon" :class="[isMini ? 'text-icon-small' : 'text-icon-large']" />
      <p
        class="w-full ellipsis-text text-center text-12px"
        :class="[isMini ? 'h-0 pt-0' : 'h-20px pt-4px']"
      >
        {{ label }}
      </p>
    </div>
  </DefineMixMenuItem>
  <!-- define component end: MixMenuItem -->

  <div class="h-full flex-col-stretch flex-1-hidden">
    <slot></slot>
    <SimpleScrollbar>
      <MixMenuItem
        v-for="menu in menus"
        :key="menu.key"
        :label="menu.label"
        :icon="menu.icon"
        :active="menu.key === activeMenuKey"
        :is-mini="siderCollapse"
        @click="handleClickMixMenu(menu.routeKey)"
      />
    </SimpleScrollbar>
    <MenuToggler
      arrow-icon
      :collapsed="siderCollapse"
      :z-index="99"
      :class="{ 'text-white:88 !hover:text-white': inverted }"
      @click="toggleSiderCollapse"
    />
  </div>
</template>

<style scoped>
.mix-menu-item {
  border: 1px solid rgba(var(--admin-accent-rgb), 0.03);
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.mix-menu-item:hover {
  background-color: v-bind(hoverBgColor);
  color: v-bind(hoverTextColor);
  border-color: rgba(var(--admin-accent-rgb), 0.12);
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(var(--admin-accent-rgb), 0.08);
}

.selected-mix-menu {
  background-color: v-bind(selectedBgColor);
  border-color: rgba(var(--admin-accent-rgb), 0.16);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.64),
    0 12px 24px rgba(var(--admin-accent-rgb), 0.1);
  animation: mix-menu-selected-in 0.2s ease-out;
}

@keyframes mix-menu-selected-in {
  from {
    opacity: 0.88;
    transform: scale(0.98);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

html.dark .mix-menu-item {
  border-color: rgba(255, 255, 255, 0.04);
  color: rgba(224, 232, 246, 0.82);
}

html.dark .mix-menu-item:hover {
  border-color: rgba(255, 255, 255, 0.08);
}
</style>
