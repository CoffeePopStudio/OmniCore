<template>
  <div>
    <n-h2>回滚</n-h2>
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
        <n-space>
          <n-button type="primary" :loading="previewLoading" @click="handlePreview">预览回滚</n-button>
        </n-space>
      </n-space>
    </n-card>

    <n-card v-if="previewData.length > 0" title="预览结果" style="margin-bottom: 16px;">
      <n-data-table :columns="previewColumns" :data="previewData" :bordered="true" />
      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <n-button type="warning" @click="showConfirmModal = true">确认执行</n-button>
      </div>
    </n-card>

    <n-modal v-model:show="showConfirmModal" preset="dialog" title="确认回滚" positive-text="确认执行" negative-text="取消" @positive-click="handleExecute" @negative-click="showConfirmModal = false">
      <p>确认执行回滚操作？此操作不可逆。</p>
    </n-modal>

    <n-card v-if="executing" title="执行进度" style="margin-bottom: 16px;">
      <n-progress :percentage="progress" :indicator-placement="'inside'" processing />
    </n-card>

    <n-card v-if="resultSummary" title="执行结果" style="margin-bottom: 16px;">
      <n-result status="success" title="回滚完成">
        <template #default>
          <p>{{ resultSummary }}</p>
        </template>
      </n-result>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import type { DataTableColumn } from 'naive-ui'
import { rollbackPreview, rollbackExecute, rollbackProgress } from '../api/client'

const route = useRoute()

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

const previewLoading = ref(false)
const previewData = ref<any[]>([])
const showConfirmModal = ref(false)
const executing = ref(false)
const progress = ref(0)
const resultSummary = ref('')

let progressTimer: ReturnType<typeof setInterval> | null = null

const previewColumns: DataTableColumn[] = [
  { title: '坐标', key: 'coords', width: 160 },
  { title: '操作摘要', key: 'summary', width: 200 },
  { title: '当前方块', key: 'currentBlock', width: 160 },
  { title: '回滚后方块', key: 'rollbackBlock', width: 160 },
]

async function handlePreview() {
  previewLoading.value = true
  try {
    const res = await rollbackPreview({
      player: filters.player || undefined,
      time: filters.time,
      world: filters.world || undefined,
      radius: filters.radius ? Number(filters.radius) : undefined,
      coords: filters.coords || undefined,
      blockType: filters.blockType || undefined,
    })
    previewData.value = res.data.records || []
  } catch (err) {
    console.error('预览失败', err)
  } finally {
    previewLoading.value = false
  }
}

async function handleExecute() {
  showConfirmModal.value = false
  executing.value = true
  progress.value = 0
  try {
    const res = await rollbackExecute({
      player: filters.player || undefined,
      time: filters.time,
      world: filters.world || undefined,
      radius: filters.radius ? Number(filters.radius) : undefined,
      coords: filters.coords || undefined,
      blockType: filters.blockType || undefined,
    })
    const ticket = res.data.ticket
    if (ticket) {
      progressTimer = setInterval(async () => {
        try {
          const pRes = await rollbackProgress(ticket)
          progress.value = pRes.data.progress || 0
          if (progress.value >= 100) {
            if (progressTimer) {
              clearInterval(progressTimer)
              progressTimer = null
            }
            executing.value = false
            resultSummary.value = pRes.data.summary || '回滚完成'
          }
        } catch {
          if (progressTimer) {
            clearInterval(progressTimer)
            progressTimer = null
          }
          executing.value = false
        }
      }, 1000)
    } else {
      progress.value = 100
      executing.value = false
      resultSummary.value = '回滚执行完成'
    }
  } catch (err) {
    console.error('执行失败', err)
    executing.value = false
  }
}

onUnmounted(() => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
})
</script>
