<script setup lang="ts">
import { computed } from 'vue';
import type { Component } from 'vue';
import { getPaletteColorByNumber, mixColor } from '@sa/color';
import { loginModuleRecord } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';
import PwdLogin from './modules/pwd-login.vue';
import CodeLogin from './modules/code-login.vue';
import Register from './modules/register.vue';
import ResetPwd from './modules/reset-pwd.vue';
import BindWechat from './modules/bind-wechat.vue';

interface Props {
  /** The login module */
  module?: UnionKey.LoginModule;
}

const props = defineProps<Props>();

const appStore = useAppStore();
const themeStore = useThemeStore();

interface LoginModule {
  label: App.I18n.I18nKey;
  component: Component;
}

const moduleMap: Record<UnionKey.LoginModule, LoginModule> = {
  'pwd-login': { label: loginModuleRecord['pwd-login'], component: PwdLogin },
  'code-login': { label: loginModuleRecord['code-login'], component: CodeLogin },
  register: { label: loginModuleRecord.register, component: Register },
  'reset-pwd': { label: loginModuleRecord['reset-pwd'], component: ResetPwd },
  'bind-wechat': { label: loginModuleRecord['bind-wechat'], component: BindWechat }
};

const activeModule = computed(() => moduleMap[props.module || 'pwd-login']);

const bgThemeColor = computed(() =>
  themeStore.darkMode ? getPaletteColorByNumber(themeStore.themeColor, 600) : themeStore.themeColor
);

const bgColor = computed(() => {
  const COLOR_WHITE = '#ffffff';

  const ratio = themeStore.darkMode ? 0.5 : 0.2;

  return mixColor(COLOR_WHITE, themeStore.themeColor, ratio);
});

const loginShellStyle = computed(() => ({
  background: themeStore.darkMode
    ? 'radial-gradient(circle at top left, rgba(56, 124, 255, 0.18) 0%, rgba(56, 124, 255, 0) 24%), radial-gradient(circle at bottom right, rgba(0, 198, 255, 0.12) 0%, rgba(0, 198, 255, 0) 22%), linear-gradient(135deg, #020304 0%, #080a0d 42%, #11161c 100%)'
    : 'radial-gradient(circle at top left, rgba(15, 111, 255, 0.22) 0%, rgba(15, 111, 255, 0) 28%), linear-gradient(135deg, #edf5ff 0%, #f5faff 42%, #deebff 100%)'
}));
</script>

<template>
  <div class="login-shell relative size-full flex overflow-hidden" :style="loginShellStyle">
    <WaveBg :theme-color="bgThemeColor" />
    <div class="login-aurora login-aurora--amber" />
    <div class="login-aurora login-aurora--spice" />
    <div class="relative z-3 flex-1 flex-col-center px-18px py-24px md:px-40px">
      <div class="login-brand-panel w-full" style="max-width: 1120px;">
        <div class="login-panel-grid">
          <section class="login-brand-column">
            <div class="flex-y-center gap-16px mb-32px">
              <SystemLogo class="size-72px" />
              <div>
                <h1 class="text-36px font-700 login-brand-title">{{ $t('system.title') }}</h1>
                <p class="text-14px mt-4px login-brand-subtitle">智慧餐饮管理中枢</p>
              </div>
            </div>
            <p class="text-16px leading-relaxed login-brand-copy">
              前厅、后厨、收银与会员运营，统一在一套顺手的经营后台里完成。
            </p>
            <div class="mt-40px flex flex-col gap-16px">
              <div class="login-feature-item flex items-center gap-12px">
                <div class="login-feature-icon size-36px rd-8px flex-center text-15px font-700">点</div>
                <span class="text-14px login-feature-text">快速开台点单，前厅动线更顺</span>
              </div>
              <div class="login-feature-item flex items-center gap-12px">
                <div class="login-feature-icon size-36px rd-8px flex-center text-15px font-700">营</div>
                <span class="text-14px login-feature-text">经营数据即时回看，忙时也能快速判断</span>
              </div>
              <div class="login-feature-item flex items-center gap-12px">
                <div class="login-feature-icon size-36px rd-8px flex-center text-15px font-700">单</div>
                <span class="text-14px login-feature-text">订单、后厨、收银串成一条完整链路</span>
              </div>
            </div>
          </section>
          <section class="login-form-column">
            <div class="login-form-shell">
              <div class="login-form-shell__kicker">CONTROL ACCESS</div>
              <div class="login-form-shell__meta">
                <span>前厅</span>
                <span>后厨</span>
                <span>收银</span>
              </div>
            </div>
            <NCard :bordered="false" class="rd-16px login-card">
              <div class="px-16px py-8px lt-sm:px-8px">
                <header class="flex-y-center justify-between mb-20px">
                  <SystemLogo class="size-56px md:hidden" />
                  <h3 class="text-28px text-primary font-600 lt-sm:text-22px login-card-title">{{ $t('system.title') }}</h3>
                  <div class="w-56px md:hidden"></div>
                </header>
                <main>
                  <h3 class="text-18px text-primary font-medium mb-20px">{{ $t(activeModule.label) }}</h3>
                  <Transition :name="themeStore.page.animateMode" mode="out-in" appear>
                    <component :is="activeModule.component" />
                  </Transition>
                </main>
              </div>
            </NCard>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  isolation: isolate;
}

.login-aurora {
  position: absolute;
  z-index: 1;
  border-radius: 999px;
  filter: blur(18px);
  opacity: 0.55;
  pointer-events: none;
}

.login-aurora--amber {
  top: 8%;
  left: 10%;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(15, 111, 255, 0.24) 0%, rgba(15, 111, 255, 0) 72%);
}

.login-aurora--spice {
  right: 8%;
  bottom: 12%;
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(20, 163, 255, 0.18) 0%, rgba(20, 163, 255, 0) 72%);
}

.login-brand-panel {
  position: relative;
  padding: 38px 42px;
  border-radius: 32px;
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(249, 253, 255, 0.84), rgba(242, 248, 255, 0.58));
  border: 1px solid rgba(15, 111, 255, 0.12);
  box-shadow:
    0 28px 60px rgba(15, 57, 119, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(18px);
}

.login-panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(380px, 460px);
  gap: 32px;
  align-items: stretch;
}

.login-brand-column {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding-right: 8px;
}

.login-form-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  position: relative;
  padding-left: 18px;
}

.login-form-column::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 1px;
  background: linear-gradient(180deg, rgba(15, 111, 255, 0), rgba(15, 111, 255, 0.2), rgba(15, 111, 255, 0));
}

.login-form-shell {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 6px;
}

.login-form-shell__kicker {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: rgba(15, 62, 124, 0.64);
}

.login-form-shell__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.login-form-shell__meta span {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #0f6fff;
  background: rgba(15, 111, 255, 0.08);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.login-brand-title {
  color: #123e7c;
}

.login-brand-subtitle {
  color: rgba(18, 62, 124, 0.64);
}

.login-brand-copy,
.login-feature-text {
  color: rgba(21, 44, 76, 0.76);
}

.login-feature-item {
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.64);
  border: 1px solid rgba(15, 111, 255, 0.1);
}

.login-feature-icon {
  background: linear-gradient(180deg, rgba(15, 111, 255, 0.16), rgba(20, 163, 255, 0.12));
  color: rgb(var(--primary-color));
}

.login-card {
  width: 100%;
  backdrop-filter: blur(22px);
  background:
    radial-gradient(circle at top, rgba(15, 111, 255, 0.1), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(243, 249, 255, 0.78)) !important;
  border: 1px solid rgba(15, 111, 255, 0.12);
  box-shadow:
    0 24px 54px rgba(15, 57, 119, 0.16),
    0 10px 24px rgba(8, 27, 58, 0.08);
}

.login-card-title {
  color: #123e7c;
}

html.dark .login-aurora--amber {
  background: radial-gradient(circle, rgba(72, 130, 255, 0.22) 0%, rgba(72, 130, 255, 0) 70%);
  opacity: 0.72;
}

html.dark .login-aurora--spice {
  background: radial-gradient(circle, rgba(0, 199, 255, 0.16) 0%, rgba(0, 199, 255, 0) 72%);
  opacity: 0.58;
}

html.dark .login-brand-panel {
  background:
    radial-gradient(circle at top right, rgba(52, 123, 255, 0.12), transparent 26%),
    linear-gradient(180deg, rgba(10, 12, 15, 0.88), rgba(15, 18, 24, 0.8));
  border-color: rgba(109, 146, 255, 0.16);
  box-shadow:
    0 32px 64px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .login-form-column::before {
  background: linear-gradient(180deg, rgba(72, 130, 255, 0), rgba(72, 130, 255, 0.22), rgba(0, 199, 255, 0));
}

html.dark .login-form-shell__kicker {
  color: rgba(187, 202, 230, 0.56);
}

html.dark .login-form-shell__meta span {
  color: #dbe8ff;
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(124, 157, 255, 0.16);
}

html.dark .login-brand-title,
html.dark .login-card-title {
  color: #f5f7fb;
}

html.dark .login-brand-subtitle {
  color: rgba(205, 214, 232, 0.68);
}

html.dark .login-brand-copy,
html.dark .login-feature-text {
  color: rgba(223, 229, 241, 0.78);
}

html.dark .login-feature-item {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.02));
  border-color: rgba(120, 150, 255, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

html.dark .login-feature-icon {
  background: linear-gradient(180deg, rgba(74, 128, 255, 0.18), rgba(0, 199, 255, 0.12));
  color: #dce7ff;
}

html.dark .login-card {
  background:
    radial-gradient(circle at top, rgba(72, 130, 255, 0.14), transparent 26%),
    linear-gradient(180deg, rgba(12, 14, 18, 0.94), rgba(18, 21, 27, 0.9)) !important;
  border-color: rgba(118, 150, 255, 0.14);
  box-shadow:
    0 20px 54px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .login-card :deep(h3),
html.dark .login-card :deep(label),
html.dark .login-card :deep(.n-form-item-label__text),
html.dark .login-card :deep(.n-input__placeholder),
html.dark .login-card :deep(.n-checkbox__label) {
  color: rgba(238, 242, 248, 0.9);
}

@media (max-width: 767px) {
  .login-brand-panel {
    padding: 22px 18px;
  }

  .login-panel-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .login-brand-column {
    padding-right: 0;
  }

  .login-form-column {
    padding-left: 0;
  }

  .login-form-column::before {
    display: none;
  }

  .login-form-shell {
    padding: 0;
    flex-direction: column;
    align-items: flex-start;
  }

  .login-card {
    backdrop-filter: none;
    background: rgba(255, 255, 255, 0.96) !important;
  }

  html.dark .login-card {
    background: linear-gradient(180deg, rgba(10, 12, 16, 0.98), rgba(17, 20, 26, 0.94)) !important;
  }
}
</style>
