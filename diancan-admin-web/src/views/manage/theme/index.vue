<script setup lang="ts">
import { computed } from 'vue';
import { NCard, NSpace } from 'naive-ui';
import ThemePresetPanel from '@/components/common/theme-preset-panel.vue';
import { getOfficialThemePresets } from '@/theme/preset/shared';

defineOptions({
  name: 'ManageTheme'
});

const themePresets = computed(() => getOfficialThemePresets());
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="theme-hero">
      <div class="theme-hero__eyebrow">THEME STUDIO</div>
      <div class="theme-hero__head">
        <div>
          <h2 class="theme-hero__title">把管理端主题切换、品牌色氛围和暗夜风格集中维护在一张主题工作台里</h2>
          <p class="theme-hero__desc">
            适合按门店值班、岗位场景和运营偏好切换整体视觉方案。当前选择会立即作用于管理端界面，并自动保存到系统配置。
          </p>
        </div>
        <div class="theme-hero__badge">
          <span>当前预设数</span>
          <strong>{{ themePresets.length }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" class="theme-workbench-card">
      <ThemePresetPanel
        :presets="themePresets"
        title="管理端主题设置"
        description="蓝色适合作为常规运营主方案，其余绿色、暗黑、紫色、红色、橙色可用于差异化值班场景。切换后会同步更新组件主题、加载态和浏览器品牌色。"
      />
    </NCard>
  </NSpace>
</template>

<style scoped>
.theme-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.2), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(228, 239, 255, 0.98)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.theme-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.theme-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.theme-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.theme-hero__desc {
  max-width: 780px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.theme-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.theme-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.theme-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.theme-workbench-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.98)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.theme-workbench-card :deep(.theme-preset-panel__title),
.theme-workbench-card :deep(.theme-preset-card__name) {
  color: color-mix(in srgb, var(--admin-accent-strong) 56%, #1b2d45);
}

.theme-workbench-card :deep(.n-card-header) {
  padding-bottom: 10px;
}

html.dark .theme-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .theme-workbench-card {
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.1), transparent 22%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .theme-workbench-card :deep(.theme-preset-panel__title),
html.dark .theme-workbench-card :deep(.theme-preset-card__name) {
  color: rgba(243, 247, 255, 0.98) !important;
}

html.dark .theme-workbench-card :deep(.theme-preset-panel__desc),
html.dark .theme-workbench-card :deep(.theme-preset-card__version),
html.dark .theme-workbench-card :deep(.theme-preset-card__desc) {
  color: rgba(214, 223, 242, 0.8) !important;
}

html.dark .theme-workbench-card :deep(.theme-preset-card__meta .n-tag),
html.dark .theme-workbench-card :deep(.theme-preset-card__footer .n-button) {
  box-shadow: none !important;
}

@media (max-width: 900px) {
  .theme-hero__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .theme-hero__badge {
    width: 100%;
  }
}
</style>
