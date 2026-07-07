import { fileURLToPath } from 'node:url';
import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import Unocss from '@unocss/vite';
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons';
import VueDevTools from 'vite-plugin-vue-devtools';
import vueTransitionRootValidator from 'vite-plugin-vue-transition-root-validator';
import Components from 'unplugin-vue-components/vite';
import IconsResolver from 'unplugin-icons/resolver';
import Icons from 'unplugin-icons/vite';
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers';

/**
 * 组装 Vite 插件
 *
 * @param viteEnv 环境变量
 * @param buildTime 构建时间
 * @author Henfon
 * @date 2026/07/08
 * @description 按项目当前技术栈初始化 Vue、UnoCSS、组件自动注册、SVG 图标等插件
 */
export function setupVitePlugins(viteEnv: Env.ImportMeta, buildTime: string) {
  const isDev = process.env.NODE_ENV !== 'production';

  return [
    vue(),
    vueJsx(),
    Unocss(),
    Components({
      dts: 'src/typings/components.d.ts',
      resolvers: [
        NaiveUiResolver(),
        IconsResolver({
          prefix: 'icon'
        })
      ]
    }),
    Icons({
      compiler: 'vue3',
      autoInstall: false
    }),
    createSvgIconsPlugin({
      iconDirs: [fileURLToPath(new URL('../src/assets/svg-icon', import.meta.url))],
      symbolId: 'icon-local-[dir]-[name]'
    }),
    vueTransitionRootValidator(),
    isDev &&
      VueDevTools({
        launchEditor: viteEnv.VITE_DEVTOOLS_LAUNCH_EDITOR
      }),
    {
      name: 'diancan-admin-web-build-info',
      config() {
        return {
          define: {
            __APP_BUILD_TIME__: JSON.stringify(buildTime)
          }
        };
      }
    }
  ].filter(Boolean);
}
