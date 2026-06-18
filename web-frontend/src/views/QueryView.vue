<template>
  <div>
    <n-h2>查询</n-h2>
    <n-card style="margin-bottom: 16px;">
      <n-space vertical>
        <n-grid :cols="4" x-gap="12" y-gap="12">
          <n-grid-item>
            <n-form-item label="玩家名">
              <n-input v-model:value="filters.player" placeholder="输入玩家名" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="时间">
              <n-select v-model:value="filters.time" :options="timeOptions" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="世界">
              <n-input v-model:value="filters.world" placeholder="世界名称" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="半径 / 坐标">
              <n-input v-model:value="filters.radius" placeholder="半径" style="margin-bottom: 4px;" />
              <n-input v-model:value="filters.coords" placeholder="x,y,z" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="方块类型">
              <n-input v-model:value="filters.blockType" placeholder="minecraft:stone" />
            </n-form-item>
          </n-grid-item>
        </n-grid>
        <n-button type="primary" @click="handleSearch">搜索</n-button>
      </n-space>
    </n-card>

    <n-tabs v-model:value="activeTab" type="line">
      <n-tab-pane name="blocks" tab="方块记录">
        <n-data-table :columns="blockColumns" :data="blockData" :bordered="true" :loading="loading" />
      </n-tab-pane>
      <n-tab-pane name="containers" tab="容器记录">
        <n-data-table :columns="containerColumns" :data="containerData" :bordered="true" :loading="loading" />
      </n-tab-pane>
      <n-tab-pane name="inventory" tab="背包记录">
        <n-data-table :columns="inventoryColumns" :data="inventoryData" :bordered="true" :loading="loading" />
      </n-tab-pane>
    </n-tabs>

    <div style="display: flex; justify-content: center; margin-top: 16px;">
      <n-pagination
        v-model:page="page"
        :page-count="totalPages"
        @update:page="handleSearch"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { DataTableColumn } from 'naive-ui'
import { NButton } from 'naive-ui'
import { queryBlocks, queryContainers, queryInventory } from '../api/client'

const route = useRoute()
const router = useRouter()

const timeOptions = [
  { label: '30 分钟', value: '30m' },
  { label: '1 小时', value: '1h' },
  { label: '6 小时', value: '6h' },
  { label: '1 天', value: '1d' },
]

const filters = reactive({
  player: (route.query.player as string) || '',
  time: (route.query.time as string) || '30m',
  world: '',
  radius: '',
  coords: '',
  blockType: '',
})

const activeTab = ref('blocks')
const loading = ref(false)
const page = ref(1)
const totalPages = ref(1)
const pageSize = 20

const blockData = ref<any[]>([])
const containerData = ref<any[]>([])
const inventoryData = ref<any[]>([])

function createRollbackButton(row: any) {
  return h(NButton, {
    size: 'small',
    onClick: () => {
      router.push({
        name: 'Rollback',
        query: {
          player: row.player || filters.player,
          time: filters.time,
          targetTime: row.time,
        },
      })
    },
  }, { default: () => '预览回滚到此' })
}

const blockColumns: DataTableColumn[] = [
  { title: '时间', key: 'time', width: 160 },
  { title: '玩家', key: 'player', width: 120 },
  { title: '操作', key: 'action', width: 80 },
  { title: '方块', key: 'blockType', width: 180 },
  { title: '坐标', key: 'coords', width: 160 },
  { title: '操作', key: 'actions', width: 140, render: createRollbackButton },
]

const containerColumns: DataTableColumn[] = [
  { title: '时间', key: 'time', width: 160 },
  { title: '玩家', key: 'player', width: 120 },
  { title: '操作', key: 'action', width: 80 },
  { title: '物品', key: 'item', width: 180 },
  { title: '坐标', key: 'coords', width: 160 },
  { title: '操作', key: 'actions', width: 140, render: createRollbackButton },
]

const inventoryColumns: DataTableColumn[] = [
  { title: '时间', key: 'time', width: 160 },
  { title: '玩家', key: 'player', width: 120 },
  { title: '操作', key: 'action', width: 80 },
  { title: '物品', key: 'item', width: 180 },
  { title: '槽位', key: 'slot', width: 80 },
  { title: '操作', key: 'actions', width: 140, render: createRollbackButton },
]

async function handleSearch() {
  loading.value = true
  const params = {
    player: filters.player || undefined,
    time: filters.time,
    world: filters.world || undefined,
    radius: filters.radius ? Number(filters.radius) : undefined,
    coords: filters.coords || undefined,
    blockType: filters.blockType || undefined,
    page: page.value,
    pageSize,
  }
  try {
    if (activeTab.value === 'blocks') {
      const res = await queryBlocks(params)
      blockData.value = res.data.records || []
      totalPages.value = Math.ceil((res.data.total || 0) / pageSize)
    } else if (activeTab.value === 'containers') {
      const res = await queryContainers(params)
      containerData.value = res.data.records || []
      totalPages.value = Math.ceil((res.data.total || 0) / pageSize)
    } else {
      const res = await queryInventory(params)
      inventoryData.value = res.data.records || []
      totalPages.value = Math.ceil((res.data.total || 0) / pageSize)
    }
  } catch (err) {
    console.error('查询失败', err)
  } finally {
    loading.value = false
  }
}
</script>
