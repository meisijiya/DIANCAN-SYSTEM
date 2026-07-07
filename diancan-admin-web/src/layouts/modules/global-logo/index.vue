<script setup lang="ts">
import { $t } from '@/locales';

defineOptions({
  name: 'GlobalLogo'
});

interface Props {
  /** Whether to show the title */
  showTitle?: boolean;
  /** Visual variant */
  variant?: 'default' | 'sider';
}

withDefaults(defineProps<Props>(), {
  showTitle: true,
  variant: 'default'
});
</script>

<template>
  <RouterLink to="/" class="global-logo" :class="[`global-logo--${variant}`, { 'global-logo--collapsed': !showTitle }]">
    <span class="global-logo__mark">
      <SystemLogo class="size-32px" />
    </span>
    <span v-show="showTitle" class="global-logo__copy">
      <span v-if="variant === 'sider'" class="global-logo__eyebrow">OPS CONSOLE</span>
      <h2 class="global-logo__title">
        {{ $t('system.title') }}
      </h2>
    </span>
  </RouterLink>
</template>

<style scoped>
.global-logo {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  text-decoration: none;
  color: inherit;
}

.global-logo__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.global-logo__copy {
  min-width: 0;
  max-width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.global-logo__eyebrow {
  font-size: 10px;
  line-height: 1;
  letter-spacing: 0.18em;
  color: rgba(var(--admin-accent-rgb), 0.56);
  margin-bottom: 6px;
}

.global-logo__title {
  margin: 0;
  font-size: 14px;
  line-height: 1.15;
  font-weight: 700;
  color: #123055;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.25s ease;
}

.global-logo--sider {
  padding: 18px 18px 16px;
  border-radius: 22px;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 30%),
    linear-gradient(180deg, #ffffff 0%, color-mix(in srgb, var(--admin-layout-start) 72%, white) 100%);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  box-shadow:
    0 14px 26px rgba(var(--admin-accent-rgb), 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.64);
}

.global-logo--sider .global-logo__mark {
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(var(--admin-accent-rgb), 0.18), rgba(var(--admin-accent-rgb), 0.08));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.7),
    0 6px 14px rgba(var(--admin-accent-rgb), 0.1);
}

.global-logo--collapsed {
  justify-content: center;
}

html.dark .global-logo__eyebrow {
  color: rgba(180, 197, 230, 0.56);
}

html.dark .global-logo__title {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .global-logo--sider {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 30%),
    linear-gradient(180deg, rgba(10, 14, 22, 0.96), rgba(15, 19, 30, 0.98));
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 18px 28px rgba(0, 0, 0, 0.36),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .global-logo--sider .global-logo__mark {
  background: linear-gradient(180deg, rgba(var(--admin-accent-rgb), 0.22), rgba(var(--admin-accent-rgb), 0.08));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 8px 16px rgba(0, 0, 0, 0.26);
}
</style>
