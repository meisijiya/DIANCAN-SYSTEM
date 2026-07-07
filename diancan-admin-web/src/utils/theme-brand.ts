import { getColorPalette, getPaletteColorByNumber } from '@sa/color';

function ensureThemeColorMeta() {
  let meta = document.querySelector('meta[name="theme-color"]');

  if (!meta) {
    meta = document.createElement('meta');
    meta.setAttribute('name', 'theme-color');
    document.head.appendChild(meta);
  }

  return meta;
}

function ensureFaviconLink() {
  let link = document.querySelector('link[rel="icon"]') as HTMLLinkElement | null;

  if (!link) {
    link = document.createElement('link');
    link.rel = 'icon';
    document.head.appendChild(link);
  }

  return link;
}

function createThemeFavicon(themeColor: string) {
  const color300 = getPaletteColorByNumber(themeColor, 300, true);
  const color500 = getPaletteColorByNumber(themeColor, 500, true);
  const color700 = getPaletteColorByNumber(themeColor, 700, true);
  const steamColor = getColorPalette(themeColor, true).get(200) || '#ffffff';

  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
      <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="${color300}" />
          <stop offset="55%" stop-color="${color500}" />
          <stop offset="100%" stop-color="${color700}" />
        </linearGradient>
      </defs>
      <rect x="4" y="4" width="56" height="56" rx="16" fill="url(#bg)" />
      <circle cx="47" cy="17" r="6" fill="rgba(255,255,255,0.16)" />
      <path d="M16 39c0-1.2.9-2.2 2.2-2.2h27.6c1.2 0 2.2 1 2.2 2.2 0 5.9-4.8 10.6-10.6 10.6H26.6C20.8 49.6 16 44.9 16 39Z" fill="rgba(255,255,255,0.98)" />
      <path d="M20 36.5c0-.8.7-1.5 1.5-1.5h21c.8 0 1.5.7 1.5 1.5S43.3 38 42.5 38h-21c-.8 0-1.5-.7-1.5-1.5Z" fill="rgba(255,255,255,0.82)" />
      <path d="M24 42h16c0 3.4-2.7 6.1-6.1 6.1h-3.8C26.7 48.1 24 45.4 24 42Z" fill="${steamColor}" fill-opacity="0.28" />
      <path d="M26 18c-1.8 2.8-1.4 5.2.9 7.1 2.1 1.7 2.3 3.6.7 5.8" fill="none" stroke="white" stroke-width="2.8" stroke-linecap="round" />
      <path d="M32 14c-1.9 3-1.4 5.5 1 7.5 2.1 1.8 2.3 3.7.7 6" fill="none" stroke="white" stroke-width="2.8" stroke-linecap="round" />
      <path d="M38 18c-1.8 2.8-1.4 5.2.9 7.1 2.1 1.7 2.3 3.6.7 5.8" fill="none" stroke="white" stroke-width="2.8" stroke-linecap="round" />
    </svg>
  `.trim();

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

/**
 * 更新浏览器品牌元素
 *
 * @author Henfon
 * @date 2026-07-02
 * @description 同步更新浏览器标签页图标和 theme-color，使其与当前主题色保持一致。
 * @param themeColor 当前主题色
 */
export function updateThemeBrand(themeColor: string) {
  const meta = ensureThemeColorMeta();
  meta.setAttribute('content', themeColor);

  const favicon = ensureFaviconLink();
  favicon.href = createThemeFavicon(themeColor);
}
