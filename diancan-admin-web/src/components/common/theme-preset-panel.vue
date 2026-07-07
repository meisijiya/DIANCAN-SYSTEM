<script setup lang="ts">
import { NButton, NTag } from 'naive-ui';
import { $t } from '@/locales';
import { saveAdminThemePreset } from '@/service/api/system';
import { useThemeStore } from '@/store/modules/theme';
import {
  applyThemePreset,
  getThemePresetDesc,
  getThemePresetName,
  isThemePresetActive,
  type ThemePreset
} from '@/theme/preset/shared';

defineOptions({
  name: 'ThemePresetPanel'
});

const props = withDefaults(
  defineProps<{
    presets: ThemePreset[];
    title?: string;
    description?: string;
  }>(),
  {
    title: '',
    description: ''
  }
);

const themeStore = useThemeStore();

function getThemeSchemeLabel(themeScheme: UnionKey.ThemeScheme) {
  const labelMap: Record<UnionKey.ThemeScheme, string> = {
    light: '亮色',
    dark: '暗黑',
    auto: '自动'
  };

  return labelMap[themeScheme];
}

function createPreviewStyle(preset: ThemePreset) {
  const themeColor = preset.themeColor || '#0f6fff';
  const infoColor = preset.otherColor?.info || themeColor;
  const successColor = preset.otherColor?.success || '#21a67a';
  const isDarkPreset = preset.themeScheme === 'dark';
  const surface =
    isDarkPreset
      ? 'linear-gradient(135deg, rgba(12, 18, 30, 0.96), rgba(25, 35, 56, 0.94))'
      : 'linear-gradient(135deg, color-mix(in srgb, var(--preset-primary) 10%, white), color-mix(in srgb, var(--preset-info) 14%, white))';
  const textColor = isDarkPreset ? '#eff6ff' : '#163a70';
  const chipBackground = isDarkPreset
    ? 'rgba(255, 255, 255, 0.08)'
    : 'color-mix(in srgb, white 76%, var(--preset-primary) 24%)';
  const chipTextColor = isDarkPreset ? '#eff6ff' : 'color-mix(in srgb, var(--preset-primary) 72%, #163a70)';
  const chipBorderColor = isDarkPreset ? 'rgba(255, 255, 255, 0.12)' : 'rgba(255, 255, 255, 0.72)';

  return {
    '--preset-primary': themeColor,
    '--preset-info': infoColor,
    '--preset-success': successColor,
    '--preset-surface': surface,
    '--preset-text': textColor,
    '--preset-chip-bg': chipBackground,
    '--preset-chip-text': chipTextColor,
    '--preset-chip-border': chipBorderColor
  };
}

async function handleApply(preset: ThemePreset) {
  applyThemePreset(themeStore, preset);

  const { error } = await saveAdminThemePreset(preset.id);

  if (error) {
    window.$message?.warning('主题已应用到当前浏览器，但服务端保存失败');
    return;
  }

  window.$message?.success(`${$t('theme.appearance.preset.applySuccess')}，并已保存到系统配置`);
}
</script>

<template>
  <div class="theme-preset-panel">
    <div v-if="title || description" class="theme-preset-panel__header">
      <div>
        <h3 v-if="title" class="theme-preset-panel__title">{{ title }}</h3>
        <p v-if="description" class="theme-preset-panel__desc">{{ description }}</p>
      </div>
    </div>

    <div class="theme-preset-grid">
      <article
        v-for="preset in props.presets"
        :key="preset.id"
        class="theme-preset-card"
        :class="{ 'theme-preset-card--active': isThemePresetActive(themeStore, preset) }"
      >
        <div class="theme-preset-card__preview" :style="createPreviewStyle(preset)">
          <div class="theme-preset-card__glow"></div>
          <div class="theme-preset-card__toolbar">
            <span class="theme-preset-card__dot"></span>
            <span class="theme-preset-card__dot"></span>
            <span class="theme-preset-card__dot"></span>
          </div>
          <div class="theme-preset-card__hero">
            <div class="theme-preset-card__chip">控制台</div>
            <div class="theme-preset-card__hero-line"></div>
            <div class="theme-preset-card__hero-line theme-preset-card__hero-line--short"></div>
          </div>
        </div>

        <div class="theme-preset-card__body">
          <div class="theme-preset-card__meta">
            <div>
              <div class="theme-preset-card__name">{{ getThemePresetName(preset) }}</div>
              <div class="theme-preset-card__version">v{{ preset.version }}</div>
            </div>
            <NTag size="small" round :bordered="false" :type="preset.themeScheme === 'dark' ? 'warning' : 'info'">
              {{ getThemeSchemeLabel(preset.themeScheme || 'light') }}
            </NTag>
          </div>

          <p class="theme-preset-card__desc">{{ getThemePresetDesc(preset) }}</p>

          <div class="theme-preset-card__footer">
            <div class="theme-preset-card__swatches">
              <span :style="{ backgroundColor: preset.themeColor }"></span>
              <span :style="{ backgroundColor: preset.otherColor?.info || preset.themeColor }"></span>
              <span :style="{ backgroundColor: preset.otherColor?.success || '#21a67a' }"></span>
            </div>
            <NButton
              type="primary"
              size="small"
              round
              :ghost="!isThemePresetActive(themeStore, preset)"
              @click="handleApply(preset)"
            >
              {{ isThemePresetActive(themeStore, preset) ? '当前使用' : $t('theme.appearance.preset.apply') }}
            </NButton>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.theme-preset-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.theme-preset-panel__title {
  margin: 0;
  font-size: 18px;
  color: #123055;
}

.theme-preset-panel__desc {
  margin: 8px 0 0;
  color: rgba(21, 44, 76, 0.72);
  line-height: 1.7;
}

.theme-preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.theme-preset-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.98));
  box-shadow: 0 18px 34px rgba(var(--admin-accent-rgb), 0.08);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.theme-preset-card:hover {
  transform: translateY(-3px);
  border-color: rgba(var(--admin-accent-rgb), 0.18);
  box-shadow: 0 24px 40px rgba(var(--admin-accent-rgb), 0.12);
}

.theme-preset-card--active {
  border-color: rgba(var(--admin-accent-rgb), 0.3);
  box-shadow:
    0 28px 48px rgba(var(--admin-accent-rgb), 0.16),
    0 0 0 1px rgba(var(--admin-accent-rgb), 0.08);
}

.theme-preset-card__preview {
  position: relative;
  padding: 14px;
  min-height: 142px;
  color: var(--preset-text);
  background: var(--preset-surface);
}

.theme-preset-card__glow {
  position: absolute;
  right: -26px;
  top: -30px;
  width: 112px;
  height: 112px;
  border-radius: 999px;
  background: radial-gradient(circle, color-mix(in srgb, var(--preset-primary) 42%, transparent), transparent 68%);
}

.theme-preset-card__toolbar {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 6px;
}

.theme-preset-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--preset-text) 22%, white);
}

.theme-preset-card__hero {
  position: relative;
  z-index: 1;
  margin-top: 14px;
}

.theme-preset-card__chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--preset-chip-bg);
  border: 1px solid var(--preset-chip-border);
  color: var(--preset-chip-text);
  font-size: 12px;
  font-weight: 700;
}

.theme-preset-card__hero-line {
  margin-top: 12px;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--preset-primary), color-mix(in srgb, var(--preset-info) 78%, white));
}

.theme-preset-card__hero-line--short {
  width: 64%;
  height: 8px;
  opacity: 0.72;
}

.theme-preset-card__body {
  padding: 16px;
}

.theme-preset-card__meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.theme-preset-card__name {
  font-size: 16px;
  font-weight: 700;
  color: #173358;
}

.theme-preset-card__version {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(21, 44, 76, 0.5);
}

.theme-preset-card__desc {
  min-height: 46px;
  margin: 12px 0 0;
  color: rgba(21, 44, 76, 0.72);
  line-height: 1.65;
  font-size: 13px;
}

.theme-preset-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
}

.theme-preset-card__swatches {
  display: flex;
  align-items: center;
  gap: 8px;
}

.theme-preset-card__swatches span {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.75);
}

html.dark .theme-preset-card {
  border-color: rgba(255, 255, 255, 0.08);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.1), transparent 22%),
    linear-gradient(180deg, rgba(10, 14, 22, 0.98), rgba(14, 19, 30, 0.98));
  box-shadow:
    0 22px 38px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

html.dark .theme-preset-card__body {
  background: transparent;
}

html.dark .theme-preset-card__preview {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

html.dark .theme-preset-card__name {
  color: #e9f1ff;
}

html.dark .theme-preset-card__version,
html.dark .theme-preset-card__desc {
  color: rgba(233, 241, 255, 0.72);
}
</style>
