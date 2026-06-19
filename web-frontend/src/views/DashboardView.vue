<template>
  <div>
    <n-h2>Dashboard</n-h2>
    <n-grid :cols="4" :x-gap="16">
      <n-gi>
        <n-card title="Server Status">
          <n-statistic :value="health.status" :status="health.status === 'ok' ? 'success' : 'error'" />
          <template #footer>
            <n-text depth="3">Version: {{ health.version || 'N/A' }}</n-text>
          </template>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="Blocks Logged">
          <n-statistic :value="blockCount" />
          <template #footer>
            <n-button text @click="loadCounts">Refresh</n-button>
          </template>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="Container Logs">
          <n-statistic :value="containerCount" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="Inventory Logs">
          <n-statistic :value="inventoryCount" />
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="User Info" style="margin-top: 16px">
      <n-descriptions :column="2">
        <n-descriptions-item label="UUID">{{ auth.uuid }}</n-descriptions-item>
        <n-descriptions-item label="Username">{{ auth.username || 'N/A' }}</n-descriptions-item>
        <n-descriptions-item label="Status">
          <n-tag type="success">Web Panel is ready</n-tag>
        </n-descriptions-item>
      </n-descriptions>
    </n-card>

    <n-card title="Quick Actions" style="margin-top: 16px">
      <n-space>
        <router-link to="/query">
          <n-button type="primary">Query Records</n-button>
        </router-link>
      </n-space>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useMessage } from 'naive-ui'

const auth = useAuthStore()
const message = useMessage()
const health = ref<{ status: string; version?: string }>({ status: 'unknown', version: '' })
const blockCount = ref(0)
const containerCount = ref(0)
const inventoryCount = ref(0)

onMounted(() => {
  loadHealth()
  loadCounts()
})

async function loadHealth() {
  try {
    const data = await api.health()
    health.value = data
  } catch {
    health.value = { status: 'unreachable', version: '' }
  }
}

async function loadCounts() {
  try {
    const [blocks, containers, inventory] = await Promise.all([
      api.statsBlocks(),
      api.statsContainers(),
      api.statsInventory(),
    ])
    blockCount.value = blocks.count
    containerCount.value = containers.count
    inventoryCount.value = inventory.count
  } catch (e: any) {
    message.error('Failed to load counts: ' + (e.message || 'Unknown error'))
  }
}
</script>
