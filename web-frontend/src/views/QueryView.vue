<template>
  <div class="query-page">
    <n-card :bordered="false" class="query-card">
      <template #header>
        <div class="query-header">
          <span>{{ pageTitle }}</span>
        </div>
      </template>

      <n-form :model="filterForm" class="filter-form" label-placement="left" label-width="80">
        <n-grid :cols="4" :x-gap="16" :y-gap="16">
          <n-grid-item>
            <n-form-item label="玩家名">
              <n-input v-model:value="filterForm.player" placeholder="输入玩家名" clearable />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="世界">
              <n-input v-model:value="filterForm.world" placeholder="输入世界名" clearable />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="方块/物品">
              <n-input v-model:value="filterForm.type" placeholder="输入类型" clearable />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="开始时间">
              <n-date-picker
                v-model:formatted-value="filterForm.timeStart"
                type="datetime"
                clearable
                value-format="yyyy-MM-dd'T'HH:mm:ss"
                placeholder="选择开始时间"
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="结束时间">
              <n-date-picker
                v-model:formatted-value="filterForm.timeEnd"
                type="datetime"
                clearable
                value-format="yyyy-MM-dd'T'HH:mm:ss"
                placeholder="选择结束时间"
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="坐标X">
              <n-input-number v-model:value="filterForm.x" clearable placeholder="X" :min="-30000000" :max="30000000" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="坐标Y">
              <n-input-number v-model:value="filterForm.y" clearable placeholder="Y" :min="-64" :max="320" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="坐标Z">
              <n-input-number v-model:value="filterForm.z" clearable placeholder="Z" :min="-30000000" :max="30000000" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="半径">
              <n-input-number v-model:value="filterForm.radius" clearable placeholder="半径" :min="0" :max="1000" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="每页">
              <n-select
                v-model:value="filterForm.pageSize"
                :options="pageSizeOptions"
                :default-value="20"
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item :span="2">
            <n-form-item>
              <n-space>
                <n-button type="primary" @click="handleSearch" :loading="loading">
                  查询
                </n-button>
                <n-button @click="handleReset">重置</n-button>
              </n-space>
            </n-form-item>
          </n-grid-item>
        </n-grid>
      </n-form>
    </n-card>

    <n-card :bordered="false" class="result-card">
      <template #header>
        <div class="result-header">
          <span>查询结果</span>
          <n-tag v-if="total > 0" type="info" size="small">
            共 {{ total }} 条记录
          </n-tag>
        </div>
      </template>

      <n-spin :show="loading">
        <n-data-table
          :columns="columns"
          :data="records"
          :bordered="false"
          :single-line="false"
          size="small"
          :row-key="(row: any) => row.id || row._id"
          @update:checked-row-keys="handleCheck"
        />

        <div class="pagination-wrapper" v-if="total > 0">
          <n-pagination
            v-model:page="currentPage"
            :page-size="filterForm.pageSize"
            :item-count="total"
            @update:page="handlePageChange"
            :page-slot="5"
            show-size-picker
            :page-sizes="[10, 20, 50, 100]"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </n-spin>
    </n-card>

    <n-drawer v-model:show="showDetail" :width="480" placement="right">
      <n-drawer-content title="详细信息" closable>
        <n-descriptions label-placement="left" :column="1" bordered size="small">
          <n-descriptions-item
            v-for="(value, key) in selectedRecord"
            :key="key"
            :label="key"
          >
            {{ formatValue(value) }}
          </n-descriptions-item>
        </n-descriptions>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { DataTableColumn } from 'naive-ui'
import { format } from 'date-fns'
import { NButton, NTag } from 'naive-ui'
import { api } from '../api/client'
import type { QueryParams } from '../api/client'

const route = useRoute()
const message = useMessage()

const queryType = computed(() => (route.meta.queryType as string) || 'blocks')
const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const showDetail = ref(false)
const selectedRecord = ref<any>({})

const pageTitle = computed(() => {
  const map: Record<string, string> = {
    blocks: '方块记录查询',
    containers: '容器记录查询',
    inventory: '背包记录查询',
  }
  return map[queryType.value] || '记录查询'
})

const pageSizeOptions = [
  { label: '10 条/页', value: 10 },
  { label: '20 条/页', value: 20 },
  { label: '50 条/页', value: 50 },
  { label: '100 条/页', value: 100 },
]

const filterForm = reactive({
  player: '',
  world: '',
  type: '',
  timeStart: null as string | null,
  timeEnd: null as string | null,
  x: null as number | null,
  y: null as number | null,
  z: null as number | null,
  radius: null as number | null,
  pageSize: 20,
})

const columns = computed<DataTableColumn[]>(() => {
  const base: DataTableColumn[] = [
    { title: '玩家', key: 'player', width: 120, ellipsis: { tooltip: true } },
    { title: queryType.value === 'blocks' ? '方块' : '物品', key: 'blockType' || 'itemType', width: 140, ellipsis: { tooltip: true } },
    { title: '世界', key: 'world', width: 140, ellipsis: { tooltip: true } },
    {
      title: '时间',
      key: 'time',
      width: 180,
      render(row: any) {
        try {
          return format(new Date(row.time), 'yyyy-MM-dd HH:mm:ss')
        } catch {
          return row.time || '-'
        }
      },
    },
    {
      title: '操作',
      key: 'actions',
      width: 80,
      render(row: any) {
        return h(NButton, {
          size: 'tiny',
          quaternary: true,
          type: 'primary',
          onClick: () => showRecordDetail(row),
        }, { default: () => '详情' })
      },
    },
  ]
  return base
})

function buildParams(): QueryParams {
  const params: QueryParams = {}
  if (filterForm.player) params.player = filterForm.player
  if (filterForm.world) params.world = filterForm.world
  if (filterForm.type) params.type = filterForm.type
  if (filterForm.timeStart) params.timeStart = filterForm.timeStart
  if (filterForm.timeEnd) params.timeEnd = filterForm.timeEnd
  if (filterForm.x !== null) params.x = filterForm.x
  if (filterForm.y !== null) params.y = filterForm.y
  if (filterForm.z !== null) params.z = filterForm.z
  if (filterForm.radius !== null) params.radius = filterForm.radius
  params.page = currentPage.value
  params.pageSize = filterForm.pageSize
  return params
}

async function handleSearch() {
  loading.value = true
  currentPage.value = 1
  try {
    const params = buildParams()
    let result
    if (queryType.value === 'blocks') {
      result = await api.queryBlocks(params)
    } else if (queryType.value === 'containers') {
      result = await api.queryContainers(params)
    } else {
      result = await api.queryInventory(params)
    }
    records.value = result.records
    total.value = result.total
  } catch (err: any) {
    message.error(err.message || '查询失败')
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handlePageChange(page: number) {
  currentPage.value = page
  await handleSearch()
}

async function handlePageSizeChange(pageSize: number) {
  filterForm.pageSize = pageSize
  currentPage.value = 1
  await handleSearch()
}

function handleReset() {
  filterForm.player = ''
  filterForm.world = ''
  filterForm.type = ''
  filterForm.timeStart = null
  filterForm.timeEnd = null
  filterForm.x = null
  filterForm.y = null
  filterForm.z = null
  filterForm.radius = null
  currentPage.value = 1
}

function handleCheck(_rowKeys: any[]) {
}

function showRecordDetail(record: any) {
  selectedRecord.value = record
  showDetail.value = true
}

function formatValue(value: any): string {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  if (typeof value === 'boolean') return value ? '是' : '否'
  return String(value)
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.query-page {
  max-width: 1200px;
  margin: 0 auto;
}

.query-card {
  background-color: #1a1a22;
  margin-bottom: 16px;
}

.query-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.filter-form {
  margin-top: 8px;
}

.result-card {
  background-color: #1a1a22;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
</style>
