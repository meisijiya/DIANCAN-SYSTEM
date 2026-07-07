<script lang="ts" setup>
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';

defineOptions({ name: 'SearchResult' });

interface Props {
  options: App.Global.Menu[];
}

defineProps<Props>();

interface Emits {
  (e: 'enter'): void;
}

const emit = defineEmits<Emits>();

const theme = useThemeStore();

const active = defineModel<string>('path', { required: true });

async function handleMouseEnter(item: App.Global.Menu) {
  active.value = item.routePath;
}

function handleTo() {
  emit('enter');
}
</script>

<template>
  <NScrollbar>
    <div class="search-result-list pb-12px">
      <template v-for="item in options" :key="item.routePath">
        <div
          class="search-result-item mt-8px h-56px flex-y-center cursor-pointer justify-between rounded-16px px-14px"
          :style="{
            background: item.routePath === active
              ? `linear-gradient(90deg, ${theme.themeColor}, var(--admin-accent-gradient-start))`
              : '',
            color: item.routePath === active ? '#fff' : ''
          }"
          @click="handleTo"
          @mouseenter="handleMouseEnter(item)"
        >
          <span class="search-result-item__icon" :class="{ 'search-result-item__icon--active': item.routePath === active }">
            <component :is="item.icon" />
          </span>
          <span class="ml-5px flex-1">
            {{ (item.i18nKey && $t(item.i18nKey)) || item.label }}
          </span>
          <icon-ant-design-enter-outlined class="icon mr-3px p-2px text-20px" />
        </div>
      </template>
    </div>
  </NScrollbar>
</template>

<style lang="scss" scoped>
.search-result-item {
  border: 1px solid rgba(var(--admin-accent-rgb), 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(240, 247, 255, 0.92));
  color: #123055;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.search-result-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(var(--admin-accent-rgb), 0.08);
  border-color: rgba(var(--admin-accent-rgb), 0.14);
}

.search-result-item__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  margin-right: 8px;
  background: rgba(var(--admin-accent-rgb), 0.08);
  color: var(--admin-accent-strong);
}

.search-result-item__icon--active {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

html.dark .search-result-item {
  background: linear-gradient(180deg, rgba(12, 17, 28, 0.94), rgba(8, 12, 20, 0.96));
  color: rgba(232, 238, 250, 0.92);
}

html.dark .search-result-item__icon {
  background: rgba(255, 255, 255, 0.06);
}
</style>
