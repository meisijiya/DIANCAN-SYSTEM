/** Default theme settings */
export const themeSettings: App.Theme.ThemeSetting = {
  themeScheme: 'light',
  grayscale: false,
  colourWeakness: false,
  recommendColor: false,
  themeColor: '#0f6fff',
  themeRadius: 14,
  otherColor: {
    info: '#14a3ff',
    success: '#21a67a',
    warning: '#f0a11a',
    error: '#e0564a'
  },
  isInfoFollowPrimary: true,
  layout: {
    mode: 'vertical',
    scrollMode: 'content'
  },
  page: {
    animate: true,
    animateMode: 'fade-slide'
  },
  header: {
    height: 56,
    breadcrumb: {
      visible: true,
      showIcon: true
    },
    multilingual: {
      visible: false
    },
    globalSearch: {
      visible: true
    }
  },
  tab: {
    visible: true,
    cache: true,
    height: 44,
    mode: 'chrome',
    closeTabByMiddleClick: false
  },
  fixedHeaderAndTab: true,
  sider: {
    inverted: false,
    width: 220,
    collapsedWidth: 64,
    mixWidth: 90,
    mixCollapsedWidth: 64,
    mixChildMenuWidth: 200,
    autoSelectFirstMenu: false
  },
  footer: {
    visible: true,
    fixed: false,
    height: 48,
    right: true
  },
  watermark: {
    visible: false,
    text: 'Henfon',
    enableUserName: false,
    enableTime: false,
    timeFormat: 'YYYY-MM-DD HH:mm'
  },
  tokens: {
    light: {
      colors: {
        container: 'rgb(249, 252, 255)',
        layout: 'rgb(236, 244, 255)',
        inverted: 'rgb(15, 35, 68)',
        'base-text': 'rgb(21, 44, 76)'
      },
      boxShadow: {
        header: '0 12px 30px rgb(15, 111, 255, 0.08)',
        sider: '12px 0 28px 0 rgb(15, 111, 255, 0.06)',
        tab: '0 10px 24px rgb(15, 111, 255, 0.06)'
      }
    },
    dark: {
      colors: {
        container: 'rgb(28, 28, 28)',
        layout: 'rgb(18, 18, 18)',
        'base-text': 'rgb(224, 224, 224)'
      }
    }
  }
};

/**
 * Override theme settings
 *
 * If publish new version, use `overrideThemeSettings` to override certain theme settings
 */
export const overrideThemeSettings: Partial<App.Theme.ThemeSetting> = {
  themeColor: '#0f6fff',
  themeRadius: 14,
  otherColor: {
    info: '#14a3ff',
    success: '#21a67a',
    warning: '#f0a11a',
    error: '#e0564a'
  },
  isInfoFollowPrimary: true,
  tokens: {
    light: {
      colors: {
        container: 'rgb(249, 252, 255)',
        layout: 'rgb(236, 244, 255)',
        inverted: 'rgb(15, 35, 68)',
        'base-text': 'rgb(21, 44, 76)'
      },
      boxShadow: {
        header: '0 12px 30px rgb(15, 111, 255, 0.08)',
        sider: '12px 0 28px 0 rgb(15, 111, 255, 0.06)',
        tab: '0 10px 24px rgb(15, 111, 255, 0.06)'
      }
    }
  }
};
