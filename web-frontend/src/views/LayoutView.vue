<template>
  <n-layout class="layout" has-sider position="absolute">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="220"
      :collapsed="collapsed"
      show-trigger="bar"
      @collapse="collapsed = true"
      @expand="collapsed = false"
      :native-scrollbar="false"
      class="layout-sider"
    >
      <div class="sider-header">
        <div class="logo">
          <span class="logo-icon">O</span>
          <span v-show="!collapsed" class="logo-text">OnmiCore</span>
        </div>
      </div>
      <n-menu
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="20"
        :value="activeKey"
        :options="menuOptions"
        @update:value="handleMenuSelect"
      />
    </n-layout-sider>
    <n-layout>
      <n-layout-header class="layout-header" bordered>
        <div class="header-left">
          <n-breadcrumb>
            <n-breadcrumb-item>{{ currentPageTitle }}</n-breadcrumb-item>
          </n-breadcrumb>
        </div>
        <div class="header-right">
          <n-tag v-if="authStore.isAuthenticated" type="info" size="small">
            {{ authStore.username }}
          </n-tag>
          <n-button quaternary size="small" @click="handleLogout">
            退出
          </n-button>
        </div>
      </n-layout-header>
      <n-layout-content class="layout-content" :native-scrollbar="false">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NIcon } from 'naive-ui'
import {
  Dashboard20Regular as DashboardIcon,
  Box20Regular as BoxIcon,
  Container20Regular as ContainerIcon,
  ShoppingBag20Regular as InventoryIcon,
  ArrowUndo20Regular as RollbackIcon,
} from '@vicons/fluent'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const collapsed = ref(false)

const activeKey = computed(() => route.path || '/')

const currentPageTitle = computed(() => {
  const map: Record<string, string> = {
    '/': '仪表盘',
    '/query/blocks': '方块查询',
    '/query/containers': '容器查询',
    '/query/inventory': '背包查询',
    '/rollback': '回滚操作',
  }
  return map[route.path] || 'OnmiCore'
})

function renderIcon(icon: any) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

const menuOptions = [
  {
    label: '仪表盘',
    key: '/',
    icon: renderIcon(DashboardIcon),
  },
  {
    label: '方块查询',
    key: '/query/blocks',
    icon: renderIcon(BoxIcon),
  },
  {
    label: '容器查询',
    key: '/query/containers',
    icon: renderIcon(ContainerIcon),
  },
  {
    label: '背包查询',
    key: '/query/inventory',
    icon: renderIcon(InventoryIcon),
  },
  {
    label: '回滚操作',
    key: '/rollback',
    icon: renderIcon(RollbackIcon),
  },
]

function handleMenuSelect(key: string) {
  router.push(key)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.layout-sider {
  background-color: #1a1a22;
}

.sider-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #7c3aed, #3b82f6);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #e0e0e0;
  white-space: nowrap;
}

.layout-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background-color: #1a1a22;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout-content {
  padding: 24px;
  background-color: #121218;
  height: calc(100vh - 52px);
}
</style>
