<template>
  <n-layout position="absolute" style="height: 100vh">
    <n-layout-header bordered style="height: 48px; display: flex; align-items: center; padding: 0 16px">
      <n-h3 style="margin: 0; flex: 1">OnmiCore</n-h3>
      <n-space>
        <button class="theme-toggle" @click="theme.toggle" :title="theme.isDark ? 'Switch to Light' : 'Switch to Dark'">
          <svg v-if="theme.isDark" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </button>
        <n-tag v-if="auth.isLoggedIn" type="info" size="small">{{ auth.username || auth.uuid }}</n-tag>
        <n-button v-if="auth.isLoggedIn" text @click="handleLogout">Logout</n-button>
      </n-space>
    </n-layout-header>
    <n-layout has-sider position="absolute" style="top: 48px; bottom: 0">
      <n-layout-sider bordered content-style="padding: 16px" width="200">
        <n-menu :value="activeKey" :options="menuOptions" @update:value="handleMenuSelect" />
      </n-layout-sider>
      <n-layout content-style="padding: 24px" style="overflow-y: auto; background: var(--bg-primary);">
        <router-view />
      </n-layout>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const theme = useThemeStore()

const activeKey = computed(() => route.path)

const menuOptions = [
  { label: 'Dashboard', key: '/dashboard' },
  { label: 'Query', key: '/query' },
  { label: 'Rollback', key: '/rollback' },
  { label: 'Logs', key: '/logs' },
]

function handleMenuSelect(key: string) {
  router.push(key)
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-toggle:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
  border-color: var(--border-card);
}
</style>
