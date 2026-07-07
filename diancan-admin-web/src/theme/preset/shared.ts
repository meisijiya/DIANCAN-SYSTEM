import { defu } from 'defu';
import type { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';
import { themeSettings } from '@/theme/settings';

export interface ThemePreset extends Partial<App.Theme.ThemeSetting> {
  id: string;
  name: string;
  desc: string;
  version: string;
  i18nkey?: string;
  official?: boolean;
  naiveui?: App.Theme.NaiveUIThemeOverride;
}

type ThemeStore = ReturnType<typeof useThemeStore>;

const presetModules = import.meta.glob('./*.json', { eager: true, import: 'default' });

const officialPresetOrder = [
  'ocean-blue',
  'jade-green',
  'obsidian-night',
  'iris-purple',
  'crimson-red',
  'amber-orange'
];

function getPresetOrder(id: string) {
  const order = officialPresetOrder.indexOf(id);

  return order >= 0 ? order : Number.MAX_SAFE_INTEGER;
}

export function getThemePresets() {
  return Object.entries(presetModules)
    .map(([path, presetData]) => {
      const fileName = path.split('/').pop()?.replace('.json', '') || '';

      return {
        id: fileName,
        ...(presetData as Omit<ThemePreset, 'id'>)
      } satisfies ThemePreset;
    })
    .sort((a, b) => {
      const orderDiff = getPresetOrder(a.id) - getPresetOrder(b.id);

      if (orderDiff !== 0) {
        return orderDiff;
      }

      if (a.official && !b.official) {
        return -1;
      }

      if (!a.official && b.official) {
        return 1;
      }

      return a.name.localeCompare(b.name);
    });
}

export function getOfficialThemePresets() {
  return getThemePresets().filter(preset => preset.official);
}

export function getThemePresetById(presetId: string) {
  return getThemePresets().find(preset => preset.id === presetId);
}

export function getThemePresetName(preset: ThemePreset) {
  if (!preset.i18nkey) {
    return preset.name;
  }

  try {
    const key = `${preset.i18nkey}.name` as App.I18n.I18nKey;
    const translated = $t(key);

    return translated !== key ? translated : preset.name;
  } catch {
    return preset.name;
  }
}

export function getThemePresetDesc(preset: ThemePreset) {
  if (!preset.i18nkey) {
    return preset.desc;
  }

  try {
    const key = `${preset.i18nkey}.desc` as App.I18n.I18nKey;
    const translated = $t(key);

    return translated !== key ? translated : preset.desc;
  } catch {
    return preset.desc;
  }
}

export function applyThemePreset(themeStore: ThemeStore, preset: ThemePreset) {
  const mergedPreset = defu(preset, themeSettings);
  const { themeScheme, grayscale, colourWeakness, layout, watermark, naiveui, ...rest } = mergedPreset;

  themeStore.setThemeScheme(themeScheme);
  themeStore.setGrayscale(grayscale);
  themeStore.setColourWeakness(colourWeakness);
  themeStore.setThemeLayout(layout.mode);
  themeStore.setWatermarkEnableUserName(watermark.enableUserName);
  themeStore.setWatermarkEnableTime(watermark.enableTime);

  Object.assign(themeStore, {
    ...rest,
    layout: { ...themeStore.layout, scrollMode: layout.scrollMode },
    page: { ...rest.page },
    header: { ...rest.header },
    tab: { ...rest.tab },
    sider: { ...rest.sider },
    footer: { ...rest.footer },
    watermark: { ...watermark },
    tokens: { ...rest.tokens }
  });

  themeStore.setNaiveThemeOverrides(naiveui);
}

export function isThemePresetActive(themeStore: ThemeStore, preset: ThemePreset) {
  return themeStore.themeColor === preset.themeColor && themeStore.themeScheme === preset.themeScheme;
}
