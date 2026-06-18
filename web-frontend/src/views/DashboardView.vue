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
        <n-card title="Blocks">
          <n-statistic :value="blockCount" />
          <template #footer>
            <n-button text @click="loadCounts">Refresh</n-button>
          </template>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="Container">
          <n-statistic :value="containerCount" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="Inventory">
          <n-statistic :value="inventoryCount" />
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="Quick Actions" style="margin-top: 16px">
      <n-space>
        <router-link to="/query">
          <n-button type="primary">Query Blocks</n-button>
        </router-link>
        <router-link to="/rollback">
          <n-button type="warning">Rollback</n-button>
        </router-link>
      </n-space>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/api/client'

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
      api.queryBlocks({ page: 1, pageSize: 1 }),
      api.queryContainers({ page: 1, pageSize: 1 }),
      api.queryInventory({ page: 1, pageSize: 1 }),
    ])
    blockCount.value = blocks.page * blocks.page_size
    containerCount.value = containers.page * containers.page_size
    inventoryCount.value = inventory.page * inventory.page_size
  } catch {
    // ignore
  }
}
</script>
