<template>
  <div>
    <n-h2>Query Records</n-h2>
    <n-card style="margin-bottom: 16px">
      <n-form @submit.prevent="doQuery">
        <n-grid :cols="6" :x-gap="12" :y-gap="12">
          <n-gi>
            <n-select v-model:value="queryType" :options="typeOptions" />
          </n-gi>
          <n-gi>
            <n-input v-model:value="filters.player" placeholder="Player name" />
          </n-gi>
          <n-gi>
            <n-input v-model:value="filters.type" placeholder="Block/item type" />
          </n-gi>
          <n-gi>
            <n-input v-model:value="filters.world" placeholder="World" />
          </n-gi>
          <n-gi>
            <n-input v-model:value="filters.timeFrom" placeholder="From (e.g. 2024-01-01)" />
          </n-gi>
          <n-gi>
            <n-input v-model:value="filters.timeTo" placeholder="To (e.g. 2024-12-31)" />
          </n-gi>
        </n-grid>
        <n-space style="margin-top: 12px">
          <n-button type="primary" attr-type="submit" :loading="loading">Search</n-button>
          <n-button @click="resetFilters">Reset</n-button>
        </n-space>
      </n-form>
    </n-card>

    <n-card>
      <n-data-table :columns="columns" :data="records" :loading="loading" :pagination="pagination" :bordered="true" @update:page="onPageChange" />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted } from 'vue'
import { api, type QueryResponse } from '@/api/client'
import { NTag, useMessage } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const records = ref<any[]>([])
const currentPage = ref(1)
const pageSize = 50
const queryType = ref('blocks')

const typeOptions = [
  { label: 'Blocks', value: 'blocks' },
  { label: 'Containers', value: 'containers' },
  { label: 'Inventory', value: 'inventory' },
]

const filters = reactive({
  player: '',
  type: '',
  world: '',
  timeFrom: '',
  timeTo: '',
})

const pagination = computed(() => ({
  page: currentPage.value,
  pageSize,
  showSizePicker: false,
}))

const actionColors: Record<string, string> = {
  placed: 'success',
  broke: 'error',
  withdraw: 'warning',
  deposit: 'info',
  pickup: 'primary',
  drop: 'default',
}

const columns = computed(() => {
  const baseCols = [
    { title: 'Time', key: 'timestamp', width: 160, sortable: true },
    { title: 'Player', key: 'player_name', width: 120 },
    {
      title: 'Action', key: 'action', width: 100,
      render(row: any) {
        return h(NTag, { type: (actionColors[row.action] || 'default') as any, size: 'small' }, () => row.action)
      },
    },
  ]

  if (queryType.value === 'blocks') {
    return [
      ...baseCols,
      { title: 'World', key: 'world', width: 100 },
      { title: 'Position', key: 'pos', width: 120, render(row: any) { return `${row.x}, ${row.y}, ${row.z}` } },
      { title: 'Old Block', key: 'old_block_type', width: 120 },
      { title: 'New Block', key: 'new_block_type', width: 120 },
    ]
  }

  if (queryType.value === 'containers') {
    return [
      ...baseCols,
      { title: 'World', key: 'world', width: 100 },
      { title: 'Position', key: 'pos', width: 120, render(row: any) { return `${row.x}, ${row.y}, ${row.z}` } },
      { title: 'Item', key: 'item_type', width: 120 },
      { title: 'Amount', key: 'item_amount', width: 80 },
    ]
  }

  return [
    ...baseCols,
    { title: 'Item', key: 'item_type', width: 120 },
    { title: 'Amount', key: 'item_amount', width: 80 },
  ]
})

onMounted(() => {
  doQuery()
})

async function doQuery() {
  loading.value = true
  currentPage.value = 1
  try {
    const params: any = {
      page: currentPage.value,
      pageSize,
      player: filters.player || undefined,
      world: filters.world || undefined,
      time_from: filters.timeFrom || undefined,
      time_to: filters.timeTo || undefined,
    }
    if (filters.type) {
      if (queryType.value === 'blocks') params.block_type = filters.type
      else params.item_type = filters.type
    }

    let result: QueryResponse
    if (queryType.value === 'blocks') result = await api.queryBlocks(params)
    else if (queryType.value === 'containers') result = await api.queryContainers(params)
    else result = await api.queryInventory(params)

    records.value = result.records || []
  } catch (e: any) {
    message.error(e.message || 'Query failed')
    records.value = []
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.player = ''
  filters.type = ''
  filters.world = ''
  filters.timeFrom = ''
  filters.timeTo = ''
}

function onPageChange(page: number) {
  currentPage.value = page
  doQuery()
}
</script>
