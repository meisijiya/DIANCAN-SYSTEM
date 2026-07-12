// @unocss-include
import { getColorPalette, getRgb } from '@sa/color';
import { DARK_CLASS } from '@/constants/app';
import { overrideThemeSettings, themeSettings } from '@/theme/settings';
import { localStg } from '@/utils/storage';
import { toggleHtmlClass } from '@/utils/common';
import { updateThemeBrand } from '@/utils/theme-brand';
import { $t } from '@/locales';

export function setupLoading() {
  const cachedThemeSettings = localStg.get('themeSettings');
  const themeColor =
    cachedThemeSettings?.themeColor || localStg.get('themeColor') || overrideThemeSettings.themeColor || themeSettings.themeColor;
  const darkMode = localStg.get('darkMode') || false;
  const palette = getColorPalette(themeColor);

  const { r, g, b } = getRgb(themeColor);
  const color50 = palette.get(50) || '#eef6ff';
  const color100 = palette.get(100) || '#f5f9ff';
  const color200 = palette.get(200) || '#dbeafe';
  const color300 = palette.get(300) || themeColor;
  const color500 = palette.get(500) || themeColor;
  const color700 = palette.get(700) || themeColor;
  const bgBase = darkMode ? '#04070c' : color50;
  const bgLayer = darkMode ? '#0b111b' : '#ffffff';
  const titleColor = darkMode ? 'rgba(241,246,255,0.96)' : color700;
  const subtitleColor = darkMode ? 'rgba(206,216,236,0.72)' : `rgba(${r}, ${g}, ${b}, 0.66)`;

  const primaryColor = `--primary-color: ${r} ${g} ${b}`;

  const svgCssVars = Array.from(palette.entries())
    .map(([key, value]) => `--logo-color-${key}: ${value}`)
    .join(';');

  const cssVars = `${primaryColor}; ${svgCssVars}; --loading-bg-base: ${bgBase}; --loading-bg-layer: ${bgLayer}; --loading-title-color: ${titleColor}; --loading-subtitle-color: ${subtitleColor}; --loading-logo-bowl: rgba(${r}, ${g}, ${b}, ${darkMode ? '0.28' : '0.16'}); --loading-logo-bowl-soft: rgba(${r}, ${g}, ${b}, ${darkMode ? '0.18' : '0.1'}); --loading-logo-steam: ${darkMode ? 'rgba(255,255,255,0.96)' : color100}; --loading-logo-steam-soft: ${darkMode ? 'rgba(255,255,255,0.72)' : color200}; --loading-logo-glow: rgba(255,255,255,${darkMode ? '0.18' : '0.15'})`;

  if (darkMode) {
    toggleHtmlClass(DARK_CLASS).add();
  }

  updateThemeBrand(themeColor);

  const loadingClasses = [
    'left-0 top-0',
    'left-0 bottom-0 animate-delay-500',
    'right-0 top-0 animate-delay-1000',
    'right-0 bottom-0 animate-delay-1500'
  ];

  const dot = loadingClasses
    .map(item => {
      return `<div class="absolute w-16px h-16px bg-primary rounded-8px animate-pulse ${item}"></div>`;
    })
    .join('\n');

  const loading = `
<div class="startup-loading" style="${cssVars}">
  <style>
    .startup-loading {
      position: fixed;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      background:
        radial-gradient(circle at 18% 18%, rgba(${r}, ${g}, ${b}, ${darkMode ? '0.18' : '0.12'}) 0%, transparent 30%),
        radial-gradient(circle at 82% 20%, rgba(${r}, ${g}, ${b}, ${darkMode ? '0.16' : '0.10'}) 0%, transparent 28%),
        linear-gradient(180deg, var(--loading-bg-base), var(--loading-bg-layer));
      font-family: "Segoe UI Variable Display", "Bahnschrift", "PingFang SC", "Microsoft YaHei", sans-serif;
    }

    .startup-loading__panel {
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 24px;
      text-align: center;
    }

    .startup-loading__logo {
      width: 128px;
      height: 128px;
    }

    .startup-loading__spinner {
      position: relative;
      width: 56px;
      height: 56px;
      margin: 36px 0 20px;
    }

    .startup-loading__title {
      margin: 0;
      font-size: 28px;
      font-weight: 600;
      color: var(--loading-title-color);
    }

    .startup-loading__desc {
      font-size: 12px;
      color: var(--loading-subtitle-color);
      margin-top: 10px;
    }

    @media (max-width: 640px) {
      .startup-loading__logo {
        width: 104px;
        height: 104px;
      }
    }
  </style>
  <div class="startup-loading__panel">
    <div class="startup-loading__logo">
      ${getLogoSvg()}
    </div>
    <div class="startup-loading__spinner">
      <div class="relative h-full animate-spin">
        ${dot}
      </div>
    </div>
    <h2 class="startup-loading__title">${$t('system.title')}</h2>
    <p class="startup-loading__desc">正在加载...</p>
  </div>
</div>`;

  const app = document.getElementById('app');

  if (app) {
    app.innerHTML = loading;
  }
}

function getLogoSvg() {
  const logoSvg = `<svg
        width="100%"
        height="100%"
        viewBox="0 0 100 100"
        xmlns="http://www.w3.org/2000/svg"
      >
        <rect x="5" y="5" width="90" height="90" rx="24" ry="24" fill="url(#bgGradient)" />
        <circle cx="74" cy="26" r="10" fill="var(--loading-logo-glow)" />
        <path
          d="M24 61c0-1.7 1.3-3 3-3h46c1.7 0 3 1.3 3 3 0 8.8-7.2 16-16 16H40c-8.8 0-16-7.2-16-16Z"
          fill="rgba(255,255,255,0.98)"
        />
        <path
          d="M29 57.5c0-1.1.9-2 2-2h38c1.1 0 2 .9 2 2s-.9 2-2 2H31c-1.1 0-2-.9-2-2Z"
          fill="rgba(255,255,255,0.82)"
        />
        <path
          d="M32 65h36c0 5.5-4.5 10-10 10H42c-5.5 0-10-4.5-10-10Z"
          fill="var(--loading-logo-bowl)"
        />
        <path
          d="M35 67.5h30c0 4-3.2 7.2-7.2 7.2H42.2c-4 0-7.2-3.2-7.2-7.2Z"
          fill="var(--loading-logo-bowl-soft)"
        />
        <path
          d="M39 28c-2.8 4.5-2.2 8.3 1.4 11.3 3.2 2.7 3.5 5.5 1.1 9"
          fill="none"
          stroke="var(--loading-logo-steam)"
          stroke-width="4.2"
          stroke-linecap="round"
        />
        <path
          d="M50 22c-3 4.8-2.2 8.8 1.6 12 3.4 2.8 3.7 5.8 1.1 9.5"
          fill="none"
          stroke="var(--loading-logo-steam)"
          stroke-width="4.2"
          stroke-linecap="round"
        />
        <path
          d="M61 28c-2.8 4.5-2.2 8.3 1.4 11.3 3.2 2.7 3.5 5.5 1.1 9"
          fill="none"
          stroke="var(--loading-logo-steam)"
          stroke-width="4.2"
          stroke-linecap="round"
        />
        <path d="M69 24 79 34" fill="none" stroke="var(--loading-logo-steam)" stroke-width="3.2" stroke-linecap="round" />
        <path d="M65 20 75 30" fill="none" stroke="var(--loading-logo-steam-soft)" stroke-width="2" stroke-linecap="round" />
        <defs>
          <linearGradient id="bgGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="var(--logo-color-300)" />
            <stop offset="55%" stop-color="var(--logo-color-500)" />
            <stop offset="100%" stop-color="var(--logo-color-700)" />
          </linearGradient>
        </defs>
      </svg>
  `;

  return logoSvg;
}
