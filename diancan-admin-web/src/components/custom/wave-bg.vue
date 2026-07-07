<script lang="ts" setup>
import { computed } from 'vue';
import { getPaletteColorByNumber } from '@sa/color';

defineOptions({ name: 'WaveBg' });

interface Props {
  /** Theme color */
  themeColor: string;
}

const props = defineProps<Props>();

const color200 = computed(() => getPaletteColorByNumber(props.themeColor, 200));
const color300 = computed(() => getPaletteColorByNumber(props.themeColor, 300));
const color400 = computed(() => getPaletteColorByNumber(props.themeColor, 400));
const color500 = computed(() => getPaletteColorByNumber(props.themeColor, 500));
</script>

<template>
  <div class="absolute-lt z-1 size-full overflow-hidden">
    <svg class="size-full" preserveAspectRatio="none" viewBox="0 0 1440 900">
      <defs>
        <!-- 流光渐变1 -->
        <linearGradient id="flow1" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" :stop-color="color300" stop-opacity="0" />
          <stop offset="50%" :stop-color="color400" stop-opacity="0.6" />
          <stop offset="100%" :stop-color="color300" stop-opacity="0" />
          <animate attributeName="x1" values="-100%;100%" dur="3s" repeatCount="indefinite" />
          <animate attributeName="x2" values="0%;200%" dur="3s" repeatCount="indefinite" />
        </linearGradient>
        <!-- 流光渐变2 -->
        <linearGradient id="flow2" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" :stop-color="color200" stop-opacity="0" />
          <stop offset="50%" :stop-color="color300" stop-opacity="0.5" />
          <stop offset="100%" :stop-color="color200" stop-opacity="0" />
          <animate attributeName="x1" values="-100%;100%" dur="4s" repeatCount="indefinite" />
          <animate attributeName="x2" values="0%;200%" dur="4s" repeatCount="indefinite" />
        </linearGradient>
        <!-- 流光渐变3 -->
        <linearGradient id="flow3" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" :stop-color="color400" stop-opacity="0" />
          <stop offset="50%" :stop-color="color500" stop-opacity="0.4" />
          <stop offset="100%" :stop-color="color400" stop-opacity="0" />
          <animate attributeName="x1" values="-100%;100%" dur="5s" repeatCount="indefinite" />
          <animate attributeName="x2" values="0%;200%" dur="5s" repeatCount="indefinite" />
        </linearGradient>
        <!-- 静态填充渐变 -->
        <linearGradient id="fill1" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" :stop-color="color200" stop-opacity="0.3" />
          <stop offset="100%" :stop-color="color300" stop-opacity="0.1" />
        </linearGradient>
        <linearGradient id="fill2" x1="100%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" :stop-color="color300" stop-opacity="0.2" />
          <stop offset="100%" :stop-color="color400" stop-opacity="0.1" />
        </linearGradient>
      </defs>

      <!-- 底层静态曲线填充 -->
      <path
        d="M0,600 Q360,400 720,500 T1440,400 L1440,900 L0,900 Z"
        fill="url(#fill1)"
      />
      <path
        d="M0,700 Q360,550 720,650 T1440,500 L1440,900 L0,900 Z"
        fill="url(#fill2)"
      />

      <!-- 流光曲线1 -->
      <path
        d="M-100,500 Q300,300 600,450 Q900,600 1200,400 Q1400,280 1540,350"
        fill="none"
        stroke="url(#flow1)"
        stroke-width="3"
        stroke-linecap="round"
      />
      <!-- 流光曲线2 -->
      <path
        d="M-100,600 Q250,450 550,550 Q850,650 1150,500 Q1350,400 1540,480"
        fill="none"
        stroke="url(#flow2)"
        stroke-width="2"
        stroke-linecap="round"
      />
      <!-- 流光曲线3 -->
      <path
        d="M-100,700 Q200,550 500,650 Q800,750 1100,600 Q1300,500 1540,580"
        fill="none"
        stroke="url(#flow3)"
        stroke-width="2"
        stroke-linecap="round"
      />
      <!-- 流光曲线4 - 上方 -->
      <path
        d="M-100,300 Q350,150 700,280 Q1050,410 1400,250 L1540,200"
        fill="none"
        stroke="url(#flow2)"
        stroke-width="2"
        stroke-linecap="round"
      />
    </svg>
  </div>
</template>

<style scoped></style>
