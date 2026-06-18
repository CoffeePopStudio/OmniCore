<template>
  <n-layout position="absolute" style="height: 100vh">
    <n-layout-header bordered style="height: 48px; display: flex; align-items: center; padding: 0 16px">
      <n-h3 style="margin: 0; flex: 1">OnmiCore</n-h3>
      <n-space>
        <n-tag v-if="auth.isLoggedIn" type="info" size="small">{{ auth.username || auth.uuid }}</n-tag>
        <n-button v-if="auth.isLoggedIn" text @click="handleLogout">Logout</n-button>
      </n-space>
    </n-layout-header>
    <n-layout has-sider position="absolute" style="top: 48px; bottom: 0">
      <n-layout-sider bordered content-style="padding: 16px" width="200">
        <n-menu :value="activeKey" :options="menuOptions" @update:value="handleMenuSelect" />
      </n-layout-sider>
      <n-layout content-style="padding: 16px" style="overflow-y: auto">
        <router-view />
      </n-layout>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const activeKey = computed(() => route.path)

const menuOptions = [
  { label: 'Dashboard', key: '/dashboard' },
  { label: 'Query', key: '/query' },
  { label: 'Rollback', key: '/rollback' },
]

function handleMenuSelect(key: string) {
  router.push(key)
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
