<template>
  <n-config-provider :theme="theme.naiveTheme" :locale="zhCN" :date-locale="dateZhCN"
    :theme-overrides="theme.naiveTheme ? darkOverrides : lightOverrides">
    <n-message-provider>
      <n-notification-provider>
        <n-loading-bar-provider>
          <n-dialog-provider>
            <router-view />
          </n-dialog-provider>
        </n-loading-bar-provider>
      </n-notification-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { darkTheme, zhCN, dateZhCN } from 'naive-ui'
import type { GlobalThemeOverrides } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const theme = useThemeStore()
theme.init()

onMounted(async () => {
  const auth = useAuthStore()
  await auth.checkAutoLogin()
  checkFrontendVersion()
})

async function checkFrontendVersion() {
  try {
    const res = await fetch('/api/web-version')
    const data = await res.json()
    const cached = localStorage.getItem('web_version_cache')
    if (cached && cached !== data.version) {
      console.warn(
        `Web panel updated: ${cached} → ${data.version}. ` +
        `Clear browser cache (Ctrl+F5 / Cmd+Shift+R) if you see layout issues.`
      )
    }
    localStorage.setItem('web_version_cache', data.version)
  } catch (e) {
    // API not available (offline/dev mode) — silent
  }
}

const darkOverrides: GlobalThemeOverrides = {
  common: {
    borderRadius: '14px',
    borderColor: 'rgba(255,255,255,0.08)',
    cardColor: 'rgba(30,30,40,0.75)',
    hoverColor: 'rgba(255,255,255,0.06)',
    popoverColor: 'rgba(25,25,35,0.85)',
    modalColor: 'rgba(25,25,35,0.85)',
    inputColor: 'rgba(0,0,0,0.25)',
    inputColorDisabled: 'rgba(255,255,255,0.04)',
    tableColor: 'rgba(255,255,255,0.02)',
    tableColorHover: 'rgba(255,255,255,0.04)',
    tableColorStriped: 'rgba(255,255,255,0.02)',
    dividerColor: 'rgba(255,255,255,0.06)',
    primaryColor: '#007AFF',
    primaryColorHover: '#3395FF',
    primaryColorPressed: '#0066D6',
    primaryColorSuppl: '#007AFF',
    infoColor: '#5AC8FA',
    successColor: '#34C759',
    warningColor: '#FF9500',
    errorColor: '#FF3B30',
  },
  Layout: {
    color: 'transparent',
    headerColor: 'transparent',
    siderColor: 'transparent',
    siderBorderColor: 'transparent',
  },
  Card: {
    color: 'rgba(30,30,40,0.6)',
    borderColor: 'rgba(255,255,255,0.08)',
    actionColor: 'rgba(30,30,40,0.6)',
  },
  Menu: {
    itemColorActive: 'rgba(0,122,255,0.15)',
    itemColorActiveHover: 'rgba(0,122,255,0.2)',
    itemTextColorActive: '#007AFF',
    itemIconColorActive: '#007AFF',
    borderRadius: '10px',
  },
  Button: { borderRadius: '10px' },
  Input: { borderRadius: '10px' },
  Select: { borderRadius: '10px' },
  Tag: { borderRadius: '8px' },
  DataTable: {
    borderRadius: '14px',
    tdColor: 'transparent',
    thColor: 'rgba(255,255,255,0.04)',
  },
  Progress: { borderRadius: '6px' },
  Slider: { borderRadius: '6px' },
}

const lightOverrides: GlobalThemeOverrides = {
  common: {
    borderRadius: '14px',
    borderColor: 'rgba(0,0,0,0.08)',
    cardColor: 'rgba(255,255,255,0.7)',
    hoverColor: 'rgba(0,0,0,0.03)',
    popoverColor: 'rgba(255,255,255,0.85)',
    modalColor: 'rgba(255,255,255,0.85)',
    inputColor: 'rgba(0,0,0,0.03)',
    tableColor: 'rgba(0,0,0,0.01)',
    tableColorHover: 'rgba(0,0,122,0.03)',
    tableColorStriped: 'rgba(0,0,0,0.01)',
    dividerColor: 'rgba(0,0,0,0.06)',
    primaryColor: '#007AFF',
    primaryColorHover: '#3395FF',
    primaryColorPressed: '#0066D6',
    primaryColorSuppl: '#007AFF',
  },
  Layout: {
    color: 'transparent',
    headerColor: 'transparent',
    siderColor: 'transparent',
    siderBorderColor: 'transparent',
  },
  Card: {
    color: 'rgba(255,255,255,0.6)',
    borderColor: 'rgba(255,255,255,0.6)',
    actionColor: 'rgba(255,255,255,0.6)',
  },
  Menu: {
    itemColorActive: 'rgba(0,122,255,0.12)',
    itemColorActiveHover: 'rgba(0,122,255,0.18)',
    itemTextColorActive: '#007AFF',
    itemIconColorActive: '#007AFF',
    borderRadius: '10px',
  },
  Button: { borderRadius: '10px' },
  Input: { borderRadius: '10px' },
  Select: { borderRadius: '10px' },
  Tag: { borderRadius: '8px' },
  DataTable: {
    borderRadius: '14px',
    tdColor: 'transparent',
    thColor: 'rgba(0,0,0,0.02)',
  },
  Progress: { borderRadius: '6px' },
}
</script>

<style>
*, *::before, *::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

[data-theme="dark"] {
  --lg-bg: #0a0a0f;
  --lg-bg-secondary: #111118;
  --lg-glass: rgba(30, 30, 42, 0.55);
  --lg-glass-hover: rgba(40, 40, 55, 0.65);
  --lg-glass-border: rgba(255, 255, 255, 0.07);
  --lg-glass-border-hover: rgba(255, 255, 255, 0.12);
  --lg-glass-highlight: rgba(255, 255, 255, 0.03);
  --lg-text: #f0f0f5;
  --lg-text-secondary: rgba(255, 255, 255, 0.55);
  --lg-text-tertiary: rgba(255, 255, 255, 0.35);
  --lg-accent: #007AFF;
  --lg-accent-hover: #3395FF;
  --lg-accent-green: #34C759;
  --lg-accent-amber: #FF9500;
  --lg-accent-purple: #AF52DE;
  --lg-accent-rose: #FF3B30;
  --lg-shadow: 0 8px 32px rgba(0, 0, 0, 0.45);
  --lg-shadow-hover: 0 12px 48px rgba(0, 0, 0, 0.55);
  --lg-glow: 0 0 20px rgba(0, 122, 255, 0.08);
  --lg-blur: blur(24px);
  --lg-blur-strong: blur(40px);
}

[data-theme="light"] {
  --lg-bg: #f2f2f7;
  --lg-bg-secondary: #e8e8ed;
  --lg-glass: rgba(255, 255, 255, 0.55);
  --lg-glass-hover: rgba(255, 255, 255, 0.7);
  --lg-glass-border: rgba(0, 0, 0, 0.06);
  --lg-glass-border-hover: rgba(0, 0, 0, 0.12);
  --lg-glass-highlight: rgba(255, 255, 255, 0.8);
  --lg-text: #1c1c1e;
  --lg-text-secondary: rgba(60, 60, 67, 0.6);
  --lg-text-tertiary: rgba(60, 60, 67, 0.3);
  --lg-accent: #007AFF;
  --lg-accent-hover: #3395FF;
  --lg-accent-green: #34C759;
  --lg-accent-amber: #FF9500;
  --lg-accent-purple: #AF52DE;
  --lg-accent-rose: #FF3B30;
  --lg-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  --lg-shadow-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --lg-glow: 0 0 20px rgba(0, 122, 255, 0.06);
  --lg-blur: blur(20px);
  --lg-blur-strong: blur(32px);
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  background: var(--lg-bg);
  color: var(--lg-text);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.glass-card {
  background: var(--lg-glass);
  backdrop-filter: var(--lg-blur);
  -webkit-backdrop-filter: var(--lg-blur);
  border: 1px solid var(--lg-glass-border);
  border-radius: 16px;
  box-shadow: var(--lg-shadow);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.glass-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 16px;
  background: var(--lg-glass-highlight);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.glass-card:hover {
  border-color: var(--lg-glass-border-hover);
  box-shadow: var(--lg-shadow-hover);
  transform: translateY(-1px);
}

.glass-card:hover::before {
  opacity: 1;
}

.glass-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 22px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  border: 1px solid var(--lg-glass-border);
  background: var(--lg-glass);
  backdrop-filter: var(--lg-blur);
  -webkit-backdrop-filter: var(--lg-blur);
  color: var(--lg-text);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  user-select: none;
  text-decoration: none;
}

.glass-btn:hover {
  border-color: var(--lg-glass-border-hover);
  background: var(--lg-glass-hover);
  transform: translateY(-1px);
  box-shadow: var(--lg-shadow);
}

.glass-btn:active {
  transform: translateY(0) scale(0.98);
}

.glass-btn.primary {
  background: var(--lg-accent);
  color: #fff;
  border-color: var(--lg-accent);
}

.glass-btn.primary:hover {
  background: var(--lg-accent-hover);
  border-color: var(--lg-accent-hover);
}

.glass-btn.amber {
  background: var(--lg-accent-amber);
  color: #fff;
  border-color: var(--lg-accent-amber);
}

.glass-btn.purple {
  background: var(--lg-accent-purple);
  color: #fff;
  border-color: var(--lg-accent-purple);
}

.glass-input {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid var(--lg-glass-border);
  background: rgba(0,0,0,0.15);
  backdrop-filter: var(--lg-blur);
  -webkit-backdrop-filter: var(--lg-blur);
  color: var(--lg-text);
  font-size: 15px;
  outline: none;
  transition: all 0.25s ease;
}

.glass-input:focus {
  border-color: var(--lg-accent);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.15);
}

.glass-input::placeholder {
  color: var(--lg-text-tertiary);
}

.text-large-title { font-size: 28px; font-weight: 700; letter-spacing: -0.02em; }
.text-title-1 { font-size: 22px; font-weight: 700; letter-spacing: -0.01em; }
.text-title-2 { font-size: 18px; font-weight: 700; }
.text-title-3 { font-size: 16px; font-weight: 600; }
.text-headline { font-size: 14px; font-weight: 600; }
.text-body { font-size: 15px; }
.text-callout { font-size: 14px; }
.text-subhead { font-size: 13px; font-weight: 500; }
.text-caption { font-size: 12px; }
.text-label { font-size: 11px; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; color: var(--lg-text-tertiary); }

.n-layout-sider,
.n-layout-header {
  backdrop-filter: var(--lg-blur-strong) !important;
  -webkit-backdrop-filter: var(--lg-blur-strong) !important;
}

.n-card {
  backdrop-filter: var(--lg-blur) !important;
  -webkit-backdrop-filter: var(--lg-blur) !important;
}

[data-theme="dark"] .n-card {
  --n-color: rgba(30,30,42,0.55) !important;
  background: rgba(30,30,42,0.55) !important;
  border: 1px solid rgba(255,255,255,0.07) !important;
}

[data-theme="light"] .n-card {
  --n-color: rgba(255,255,255,0.55) !important;
  background: rgba(255,255,255,0.55) !important;
  border: 1px solid rgba(0,0,0,0.06) !important;
}

[data-theme="dark"] .n-layout-scroll-container {
  background: transparent !important;
}

[data-theme="dark"] .n-layout-sider,
[data-theme="dark"] .n-layout-header {
  background: rgba(10, 10, 15, 0.65) !important;
}

[data-theme="light"] .n-layout-sider,
[data-theme="light"] .n-layout-header {
  background: rgba(242, 242, 247, 0.7) !important;
}

.n-button {
  backdrop-filter: var(--lg-blur) !important;
}
</style>
