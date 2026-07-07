import type { GlobalThemeOverrides } from 'naive-ui';
import { defu } from 'defu';
import { addColorAlpha, getColorPalette, getPaletteColorByNumber, getRgb } from '@sa/color';
import { DARK_CLASS } from '@/constants/app';
import { toggleHtmlClass } from '@/utils/common';
import { localStg } from '@/utils/storage';
import { overrideThemeSettings, themeSettings } from '@/theme/settings';
import { themeVars } from '@/theme/vars';

/** Init theme settings */
export function initThemeSettings() {
  // Always read theme settings from localStorage so dev/prod behaviors stay consistent.
  // If want to update theme settings when publish new version, please update `overrideThemeSettings` in `src/theme/settings.ts`.

  const localSettings = localStg.get('themeSettings');

  let settings = defu(localSettings, themeSettings);

  const isOverride = localStg.get('overrideThemeFlag') === BUILD_TIME;

  if (!isOverride) {
    settings = defu(overrideThemeSettings, settings);

    localStg.set('overrideThemeFlag', BUILD_TIME);
  }

  return settings;
}

/**
 * create theme token css vars value by theme settings
 *
 * @param colors Theme colors
 * @param tokens Theme setting tokens
 * @param [recommended=false] Use recommended color. Default is `false`
 */
export function createThemeToken(
  colors: App.Theme.ThemeColor,
  tokens?: App.Theme.ThemeSetting['tokens'],
  recommended = false
) {
  const paletteColors = createThemePaletteColors(colors, recommended);

  const { light, dark } = tokens || themeSettings.tokens;

  const themeTokens: App.Theme.ThemeTokenCSSVars = {
    colors: {
      ...paletteColors,
      nprogress: paletteColors.primary,
      ...light.colors
    },
    boxShadow: {
      ...light.boxShadow
    }
  };

  const darkThemeTokens: App.Theme.ThemeTokenCSSVars = {
    colors: {
      ...themeTokens.colors,
      ...dark?.colors
    },
    boxShadow: {
      ...themeTokens.boxShadow,
      ...dark?.boxShadow
    }
  };

  return {
    themeTokens,
    darkThemeTokens
  };
}

/**
 * Create theme palette colors
 *
 * @param colors Theme colors
 * @param [recommended=false] Use recommended color. Default is `false`
 */
function createThemePaletteColors(colors: App.Theme.ThemeColor, recommended = false) {
  const colorKeys = Object.keys(colors) as App.Theme.ThemeColorKey[];
  const colorPaletteVar = {} as App.Theme.ThemePaletteColor;

  colorKeys.forEach(key => {
    const colorMap = getColorPalette(colors[key], recommended);

    colorPaletteVar[key] = colorMap.get(500)!;

    colorMap.forEach((hex, number) => {
      colorPaletteVar[`${key}-${number}`] = hex;
    });
  });

  return colorPaletteVar;
}

/**
 * Get css var by tokens
 *
 * @param tokens Theme base tokens
 */
function getCssVarByTokens(tokens: App.Theme.BaseToken) {
  const styles: string[] = [];

  function removeVarPrefix(value: string) {
    return value.replace('var(', '').replace(')', '');
  }

  function removeRgbPrefix(value: string) {
    return value.replace('rgb(', '').replace(')', '');
  }

  for (const [key, tokenValues] of Object.entries(themeVars)) {
    for (const [tokenKey, tokenValue] of Object.entries(tokenValues)) {
      let cssVarsKey = removeVarPrefix(tokenValue);
      let cssValue = tokens[key][tokenKey];

      if (key === 'colors') {
        cssVarsKey = removeRgbPrefix(cssVarsKey);
        const { r, g, b } = getRgb(cssValue);
        cssValue = `${r} ${g} ${b}`;
      }

      styles.push(`${cssVarsKey}: ${cssValue}`);
    }
  }

  const styleStr = styles.join(';');

  return styleStr;
}

function getAdminAppearanceCss(
  colors: App.Theme.ThemeColor,
  surface: { layout: string; container: string; dark: boolean },
  recommended = false
) {
  const primary = colors.primary;
  const primary400 = getPaletteColorByNumber(primary, 400, recommended);
  const primary500 = getPaletteColorByNumber(primary, 500, recommended);
  const primary600 = getPaletteColorByNumber(primary, 600, recommended);
  const primary100 = getPaletteColorByNumber(primary, 100, recommended);
  const primary200 = getPaletteColorByNumber(primary, 200, recommended);
  const { r, g, b } = getRgb(primary500);
  const layoutStart = surface.dark ? surface.layout : primary100;
  const layoutEnd = surface.dark ? surface.container : primary200;
  const accentSoft = surface.dark ? addColorAlpha(primary, 0.18) : addColorAlpha(primary, 0.16);

  return [
    `--admin-accent-rgb: ${r}, ${g}, ${b}`,
    `--admin-accent-strong: ${primary600}`,
    `--admin-accent-soft: ${accentSoft}`,
    `--admin-accent-gradient-start: ${primary400}`,
    `--admin-accent-gradient-end: ${primary600}`,
    `--admin-layout-start: ${layoutStart}`,
    `--admin-layout-end: ${layoutEnd}`
  ].join(';');
}

/**
 * Add theme vars to global
 *
 * @param tokens
 */
export function addThemeVarsToGlobal(tokens: App.Theme.BaseToken, darkTokens: App.Theme.BaseToken) {
  const cssVarStr = getCssVarByTokens(tokens);
  const darkCssVarStr = getCssVarByTokens(darkTokens);
  const adminAppearanceCss = getAdminAppearanceCss(
    {
      primary: tokens.colors.primary,
      info: tokens.colors.info,
      success: tokens.colors.success,
      warning: tokens.colors.warning,
      error: tokens.colors.error
    },
    {
      layout: tokens.colors.layout,
      container: tokens.colors.container,
      dark: false
    },
    false
  );
  const darkAdminAppearanceCss = getAdminAppearanceCss(
    {
      primary: darkTokens.colors.primary,
      info: darkTokens.colors.info,
      success: darkTokens.colors.success,
      warning: darkTokens.colors.warning,
      error: darkTokens.colors.error
    },
    {
      layout: darkTokens.colors.layout,
      container: darkTokens.colors.container,
      dark: true
    },
    false
  );

  const css = `
    :root {
      ${cssVarStr}
    }

    html {
      ${adminAppearanceCss}
    }
  `;

  const darkCss = `
    html.${DARK_CLASS} {
      ${darkCssVarStr}
      ${darkAdminAppearanceCss}
    }
  `;

  const styleId = 'theme-vars';

  const style = document.querySelector(`#${styleId}`) || document.createElement('style');

  style.id = styleId;

  style.textContent = css + darkCss;

  document.head.appendChild(style);
}

/**
 * Toggle css dark mode
 *
 * @param darkMode Is dark mode
 */
export function toggleCssDarkMode(darkMode = false) {
  const { add, remove } = toggleHtmlClass(DARK_CLASS);

  if (darkMode) {
    add();
  } else {
    remove();
  }
}

/**
 * Toggle auxiliary color modes
 *
 * @param grayscaleMode
 * @param colourWeakness
 */
export function toggleAuxiliaryColorModes(grayscaleMode = false, colourWeakness = false) {
  const htmlElement = document.documentElement;
  htmlElement.style.filter = [grayscaleMode ? 'grayscale(100%)' : '', colourWeakness ? 'invert(80%)' : '']
    .filter(Boolean)
    .join(' ');
}

type NaiveColorScene = '' | 'Suppl' | 'Hover' | 'Pressed' | 'Active';
type NaiveColorKey = `${App.Theme.ThemeColorKey}Color${NaiveColorScene}`;
type NaiveThemeColor = Partial<Record<NaiveColorKey, string>>;
interface NaiveColorAction {
  scene: NaiveColorScene;
  handler: (color: string) => string;
}

/**
 * Get naive theme colors
 *
 * @param colors Theme colors
 * @param [recommended=false] Use recommended color. Default is `false`
 */
function getNaiveThemeColors(colors: App.Theme.ThemeColor, recommended = false) {
  const colorActions: NaiveColorAction[] = [
    { scene: '', handler: color => color },
    { scene: 'Suppl', handler: color => color },
    { scene: 'Hover', handler: color => getPaletteColorByNumber(color, 500, recommended) },
    { scene: 'Pressed', handler: color => getPaletteColorByNumber(color, 700, recommended) },
    { scene: 'Active', handler: color => addColorAlpha(color, 0.1) }
  ];

  const themeColors: NaiveThemeColor = {};

  const colorEntries = Object.entries(colors) as [App.Theme.ThemeColorKey, string][];

  colorEntries.forEach(color => {
    colorActions.forEach(action => {
      const [colorType, colorValue] = color;
      const colorKey: NaiveColorKey = `${colorType}Color${action.scene}`;
      themeColors[colorKey] = action.handler(colorValue);
    });
  });

  return themeColors;
}

/**
 * Get naive theme
 *
 * @param colors Theme colors
 * @param settings Theme settings object
 * @param overrides Optional manual overrides from preset
 */
export function getNaiveTheme(
  colors: App.Theme.ThemeColor,
  settings: App.Theme.ThemeSetting,
  overrides?: GlobalThemeOverrides
) {
  const { primary: colorLoading } = colors;
  const messageSuccessBg = addColorAlpha(colors.primary, 0.12);
  const messageSuccessShadow = `0 14px 28px ${addColorAlpha(colors.primary, 0.18)}`;

  const theme: GlobalThemeOverrides = {
    common: {
      ...getNaiveThemeColors(colors, settings.recommendColor),
      borderRadius: `${settings.themeRadius}px`
    },
    LoadingBar: {
      colorLoading
    },
    Tag: {
      borderRadius: `${settings.themeRadius}px`
    },
    Message: {
      colorSuccess: messageSuccessBg,
      textColorSuccess: colors.primary,
      iconColorSuccess: colors.primary,
      boxShadowSuccess: messageSuccessShadow,
      closeIconColorSuccess: colors.primary,
      closeIconColorHoverSuccess: colors.primary,
      closeIconColorPressedSuccess: colors.primary,
      closeColorHoverSuccess: addColorAlpha(colors.primary, 0.08),
      closeColorPressedSuccess: addColorAlpha(colors.primary, 0.14)
    },
    Card: {
      borderRadius: '12px',
      paddingMedium: '20px 24px'
    }
  };

  // If there are overrides, merge them with priority
  // overrides has higher priority than auto-generated theme
  return overrides ? defu(overrides, theme) : theme;
}
