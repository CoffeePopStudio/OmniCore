<template>
  <n-layout position="absolute" style="height: 100vh">
    <n-layout-header bordered style="height: 50px; display: flex; align-items: center; padding: 0 20px">
      <div style="display: flex; align-items: center; gap: 10px; flex: 1">
        <div class="app-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
        </div>
        <span class="app-name">OnmiCore</span>
      </div>
      <div style="display: flex; align-items: center; gap: 8px">
        <button class="theme-toggle" @click="theme.toggle" :aria-label="theme.isDark ? 'Switch to light mode' : 'Switch to dark mode'">
          <svg v-if="theme.isDark" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/></svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </button>
        <span v-if="auth.isLoggedIn" class="user-badge">{{ auth.username || auth.uuid?.slice(0,8) }}</span>
        <button v-if="auth.isLoggedIn" class="logout-btn" aria-label="Logout" @click="handleLogout">Logout</button>
      </div>
    </n-layout-header>

    <n-layout has-sider position="absolute" style="top: 50px; bottom: 0">
      <n-layout-sider bordered width="210" content-style="padding: 12px">
        <n-menu :value="activeKey" :options="menuOptions" @update:value="handleMenuSelect" />
      </n-layout-sider>
      <n-layout content-style="padding: 28px 32px" style="overflow-y: auto">
        <router-view />
      </n-layout>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const theme = useThemeStore()

const activeKey = computed(() => route.path)

function renderSvg(path: string): ReturnType<typeof h> {
  return h('svg', {
    width: 16, height: 16, viewBox: '0 0 24 24',
    fill: 'none', stroke: 'currentColor',
    'stroke-width': 2, 'stroke-linecap': 'round', 'stroke-linejoin': 'round',
    innerHTML: path,
  })
}

const menuOptions = [
  {
    label: 'Dashboard', key: '/dashboard',
    icon: () => renderSvg('<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>'),
  },
  {
    label: 'Query', key: '/query',
    icon: () => renderSvg('<circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>'),
  },
  {
    label: 'Rollback', key: '/rollback',
    icon: () => renderSvg('<polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>'),
  },
  {
    label: 'Logs', key: '/logs',
    icon: () => renderSvg('<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>'),
  },
]

function handleMenuSelect(key: string) { router.push(key) }
function handleLogout() { auth.logout(); router.push('/login') }
</script>

<style scoped>
.app-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--lg-accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--lg-text);
}

.theme-toggle {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--lg-glass-border);
  background: transparent;
  color: var(--lg-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.theme-toggle:hover {
  background: var(--lg-glass-hover);
  color: var(--lg-text);
  border-color: var(--lg-glass-border-hover);
}

.user-badge {
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 20px;
  background: var(--lg-glass);
  color: var(--lg-text-secondary);
  backdrop-filter: var(--lg-blur);
  border: 1px solid var(--lg-glass-border);
}

.logout-btn {
  font-size: 13px;
  font-weight: 500;
  color: var(--lg-text-tertiary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  color: var(--lg-accent-rose);
  background: rgba(255, 59, 48, 0.1);
}
</style>
