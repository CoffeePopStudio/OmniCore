<template>
  <n-config-provider :theme="theme.naiveTheme" :locale="zhCN" :date-locale="dateZhCN">
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
import { NConfigProvider, NMessageProvider, NNotificationProvider, NLoadingBarProvider, NDialogProvider } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const theme = useThemeStore()
theme.init()

onMounted(async () => {
  const auth = useAuthStore()
  await auth.checkAutoLogin()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

:root,
[data-theme="dark"] {
  --bg-primary: #101014;
  --bg-secondary: #1a1a22;
  --bg-card: #1e1e28;
  --bg-card-hover: #252533;
  --text-primary: #e8e8ed;
  --text-secondary: #9f9fae;
  --text-muted: #66667a;
  --border-subtle: rgba(255, 255, 255, 0.06);
  --border-card: rgba(255, 255, 255, 0.08);
  --shadow-card: 0 4px 20px rgba(0, 0, 0, 0.4);
  --accent-green: #4ade80;
  --accent-blue: #60a5fa;
  --accent-amber: #fbbf24;
  --accent-purple: #a78bfa;
  --accent-rose: #fb7185;
  --avatar-bg: linear-gradient(135deg, #6366f1, #8b5cf6);
}

[data-theme="light"] {
  --bg-primary: #f5f5f7;
  --bg-secondary: #ffffff;
  --bg-card: #ffffff;
  --bg-card-hover: #fafafa;
  --text-primary: #1d1d1f;
  --text-secondary: #6e6e78;
  --text-muted: #aeaeb2;
  --border-subtle: rgba(0, 0, 0, 0.06);
  --border-card: rgba(0, 0, 0, 0.1);
  --shadow-card: 0 4px 20px rgba(0, 0, 0, 0.08);
  --accent-green: #22c55e;
  --accent-blue: #3b82f6;
  --accent-amber: #eab308;
  --accent-purple: #8b5cf6;
  --accent-rose: #f43f5e;
  --avatar-bg: linear-gradient(135deg, #4f46e5, #7c3aed);
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: var(--bg-primary);
  color: var(--text-primary);
  transition: background-color 0.3s ease, color 0.3s ease;
}
</style>
