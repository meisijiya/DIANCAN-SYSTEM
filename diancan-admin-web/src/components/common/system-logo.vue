<template>
  <div class="app-logo">
    <svg
      width="100%"
      height="100%"
      viewBox="0 0 100 100"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="5" y="5" width="90" height="90" rx="24" ry="24" fill="url(#bgGradient)" />
      <circle cx="74" cy="26" r="10" fill="rgba(255,255,255,0.15)" />
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
        fill="var(--admin-accent-soft)"
      />
      <path
        d="M39 28c-2.8 4.5-2.2 8.3 1.4 11.3 3.2 2.7 3.5 5.5 1.1 9"
        fill="none"
        stroke="white"
        stroke-width="4.2"
        stroke-linecap="round"
      />
      <path
        d="M50 22c-3 4.8-2.2 8.8 1.6 12 3.4 2.8 3.7 5.8 1.1 9.5"
        fill="none"
        stroke="white"
        stroke-width="4.2"
        stroke-linecap="round"
      />
      <path
        d="M61 28c-2.8 4.5-2.2 8.3 1.4 11.3 3.2 2.7 3.5 5.5 1.1 9"
        fill="none"
        stroke="white"
        stroke-width="4.2"
        stroke-linecap="round"
      />
      <path d="M69 24 79 34" fill="none" stroke="rgba(255,255,255,0.94)" stroke-width="3.2" stroke-linecap="round" />
      <path d="M65 20 75 30" fill="none" stroke="rgba(255,255,255,0.72)" stroke-width="2" stroke-linecap="round" />
      <defs>
        <linearGradient id="bgGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="var(--logo-color-300)" />
          <stop offset="55%" stop-color="var(--logo-color-500)" />
          <stop offset="100%" stop-color="var(--logo-color-700)" />
        </linearGradient>
      </defs>
    </svg>
  </div>
</template>

<style scoped>
.app-logo {
  --logo-color-300: rgb(var(--primary-300-color));
  --logo-color-500: rgb(var(--primary-500-color));
  --logo-color-700: rgb(var(--primary-700-color));
  filter: drop-shadow(0 10px 18px rgba(var(--admin-accent-rgb), 0.18));
}
</style>
