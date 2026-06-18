<template>
  <div class="dashboard">
    <div class="welcome-section">
      <h2>欢迎回来，{{ authStore.username }}</h2>
      <p>UUID: {{ authStore.uuid }}</p>
    </div>

    <div class="stats-grid">
      <n-card class="stat-card" :bordered="false">
        <n-statistic label="方块记录" :value="stats.blockRecords">
          <template #suffix>
            <span class="stat-unit">条</span>
          </template>
        </n-statistic>
      </n-card>
      <n-card class="stat-card" :bordered="false">
        <n-statistic label="容器记录" :value="stats.containerRecords">
          <template #suffix>
            <span class="stat-unit">条</span>
          </template>
        </n-statistic>
      </n-card>
      <n-card class="stat-card" :bordered="false">
        <n-statistic label="背包记录" :value="stats.inventoryRecords">
          <template #suffix>
            <span class="stat-unit">条</span>
          </template>
        </n-statistic>
      </n-card>
      <n-card class="stat-card" :bordered="false">
        <n-statistic label="回滚次数" :value="stats.rollbackCount">
          <template #suffix>
            <span class="stat-unit">次</span>
          </template>
        </n-statistic>
      </n-card>
    </div>

    <div class="status-section">
      <n-card title="服务器状态" :bordered="false">
        <n-space align="center">
          <n-badge :type="serverOnline ? 'success' : 'error'" dot />
          <span>{{ serverOnline ? 'API 服务在线' : 'API 服务离线' }}</span>
          <template v-if="serverVersion">
            <n-tag size="small" type="info">v{{ serverVersion }}</n-tag>
          </template>
        </n-space>
      </n-card>
    </div>

    <div class="activity-section">
      <n-card title="最近活动" :bordered="false">
        <n-data-table
          v-if="recentRecords.length > 0"
          :columns="columns"
          :data="recentRecords"
          :bordered="false"
          :single-line="false"
          size="small"
        />
        <n-empty v-else description="暂无活动记录" />
      </n-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import type { DataTableColumn } from 'naive-ui'
import { format } from 'date-fns'
import { useAuthStore } from '../stores/auth'
import { api } from '../api/client'

const authStore = useAuthStore()
const message = useMessage()

const serverOnline = ref(false)
const serverVersion = ref('')

const stats = reactive({
  blockRecords: 0,
  containerRecords: 0,
  inventoryRecords: 0,
  rollbackCount: 0,
})

const recentRecords = ref<any[]>([])

const columns: DataTableColumn[] = [
  { title: '类型', key: 'type', width: 100 },
  { title: '玩家', key: 'player', width: 120 },
  { title: '方块', key: 'blockType', width: 140 },
  { title: '世界', key: 'world', width: 140 },
  { title: '操作', key: 'action', width: 80 },
  {
    title: '时间',
    key: 'time',
    width: 180,
    render(row: any) {
      return format(new Date(row.time), 'yyyy-MM-dd HH:mm:ss')
    },
  },
]

async function loadData() {
  try {
    const health = await api.health()
    serverOnline.value = health.status === 'ok' || health.status === 'healthy'
    serverVersion.value = health.version || ''
  } catch {
    serverOnline.value = false
  }

  try {
    const blockResult = await api.queryBlocks({ page: 1, pageSize: 1 })
    stats.blockRecords = blockResult.total
  } catch {
    stats.blockRecords = 0
  }

  try {
    const containerResult = await api.queryContainers({ page: 1, pageSize: 1 })
    stats.containerRecords = containerResult.total
  } catch {
    stats.containerRecords = 0
  }

  try {
    const inventoryResult = await api.queryInventory({ page: 1, pageSize: 1 })
    stats.inventoryRecords = inventoryResult.total
  } catch {
    stats.inventoryRecords = 0
  }

  try {
    const blockResult = await api.queryBlocks({ page: 1, pageSize: 10 })
    recentRecords.value = blockResult.records.map((r: any) => ({
      ...r,
      type: '方块',
    }))
  } catch {
    recentRecords.value = []
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  margin-bottom: 24px;
}

.welcome-section h2 {
  font-size: 24px;
  font-weight: 600;
  color: #e0e0e0;
  margin-bottom: 4px;
}

.welcome-section p {
  color: #888;
  font-size: 13px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background-color: #1a1a22;
}

.stat-unit {
  font-size: 13px;
  color: #888;
  margin-left: 2px;
}

.status-section {
  margin-bottom: 24px;
}

.status-section .n-card {
  background-color: #1a1a22;
}

.activity-section .n-card {
  background-color: #1a1a22;
}
</style>
