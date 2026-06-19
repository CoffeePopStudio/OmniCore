<template>
  <div class="query-view">
    <div class="text-title-1 page-title">Query Records</div>

    <div class="glass-card filter-card">
      <form @submit.prevent="doQuery">
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
        <div class="filter-actions">
          <button class="glass-btn primary" type="submit" :disabled="loading">
            <LoadingSpinner v-if="loading" />
            <AppIcon v-else name="search" />
            Search
          </button>
          <button class="glass-btn" type="button" @click="resetFilters">Reset</button>
        </div>
      </form>
    </div>

    <div class="glass-card table-card">
      <n-data-table
        :columns="columns"
        :data="records"
        :loading="loading"
        :pagination="pagination"
        :bordered="false"
        @update:page="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted } from 'vue'
import { api } from '@/api/client'
import { NTag, useMessage } from 'naive-ui'
import type { QueryType, BlockRecord, ContainerRecord, InventoryRecord } from '@/types'
import AppIcon from '@/components/AppIcon.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const message = useMessage()
const loading = ref(false)
const records = ref<(BlockRecord | ContainerRecord | InventoryRecord)[]>([])
const currentPage = ref(1)
const pageSize = 50
const queryType = ref<QueryType>('blocks')

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
      render(row: BlockRecord | ContainerRecord | InventoryRecord) {
        return h(NTag, { type: (actionColors[row.action] || 'default') as any, size: 'small' }, () => row.action)
      },
    },
  ]

  if (queryType.value === 'blocks') {
    return [
      ...baseCols,
      { title: 'World', key: 'world', width: 100 },
      { title: 'Position', key: 'pos', width: 120, render(row: BlockRecord) { return `${row.x}, ${row.y}, ${row.z}` } },
      { title: 'Old Block', key: 'old_block_type', width: 120 },
      { title: 'New Block', key: 'new_block_type', width: 120 },
    ]
  }

  if (queryType.value === 'containers') {
    return [
      ...baseCols,
      { title: 'World', key: 'world', width: 100 },
      { title: 'Position', key: 'pos', width: 120, render(row: ContainerRecord) { return `${row.x}, ${row.y}, ${row.z}` } },
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
    const params: Record<string, any> = {
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

    let result
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

<style scoped>
.query-view {
  max-width: 1360px;
  margin: 0 auto;
}

.page-title {
  margin-bottom: 28px;
}

.filter-card {
  padding: 24px;
  margin-bottom: 28px;
  position: relative;
}

.filter-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--lg-glass-border);
}

.table-card {
  padding: 0;
  overflow: hidden;
  position: relative;
  box-shadow: var(--lg-shadow), 0 0 0 1px var(--lg-glass-highlight) inset;
}

.table-card :deep(.n-data-table) {
  --n-td-color: transparent;
  --n-th-color: transparent;
}

.table-card :deep(.n-data-table-th) {
  background: var(--lg-glass);
  backdrop-filter: var(--lg-blur);
  -webkit-backdrop-filter: var(--lg-blur);
}

.table-card :deep(.n-data-table-tr:hover) {
  background: var(--lg-glass-hover);
}

.table-card :deep(.n-data-table-pagination) {
  padding: 16px 24px;
  border-top: 1px solid var(--lg-glass-border);
  background: var(--lg-glass);
  backdrop-filter: var(--lg-blur);
  -webkit-backdrop-filter: var(--lg-blur);
}

.glass-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}
</style>
