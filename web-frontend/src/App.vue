<template>
  <NMessageProvider>
    <n-config-provider>
      <n-layout>
        <n-layout-header bordered>
          <nav style="display: flex; align-items: center; padding: 0 24px; height: 56px;">
            <h2 style="margin: 0; margin-right: 32px;">OnmiCore</h2>
            <n-space>
              <n-button tag="a" href="/dashboard" quaternary>仪表盘</n-button>
              <n-button tag="a" href="/query" quaternary>查询</n-button>
              <n-button tag="a" href="/rollback" quaternary>回滚</n-button>
            </n-space>
            <div style="flex: 1;"></div>
            <n-button quaternary @click="handleLogout" v-if="isLoggedIn">退出登录</n-button>
          </nav>
        </n-layout-header>
        <n-layout-content style="min-height: calc(100vh - 56px); padding: 24px;">
          <router-view />
        </n-layout-content>
      </n-layout>
    </n-config-provider>
  </NMessageProvider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

function handleLogout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>
