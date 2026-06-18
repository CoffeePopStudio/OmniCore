<template>
  <div class="rollback-page">
    <n-card :bordered="false" class="rollback-card">
      <template #header>
        <div class="rollback-header">
          <span>回滚操作</span>
        </div>
      </template>

      <n-form :model="formData" label-placement="left" label-width="100">
        <n-grid :cols="3" :x-gap="16" :y-gap="16">
          <n-grid-item>
            <n-form-item label="时间量">
              <n-input-number
                v-model:value="formData.timeAmount"
                :min="1"
                :max="999999"
                placeholder="输入时间量"
                clearable
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="时间单位">
              <n-select
                v-model:value="formData.timeUnit"
                :options="timeUnitOptions"
                placeholder="选择时间单位"
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="玩家">
              <n-input
                v-model:value="formData.player"
                placeholder="输入玩家名（可选）"
                clearable
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="世界">
              <n-input
                v-model:value="formData.world"
                placeholder="输入世界名（可选）"
                clearable
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="方块类型">
              <n-input
                v-model:value="formData.blockType"
                placeholder="输入方块类型（可选）"
                clearable
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="半径">
              <n-input-number
                v-model:value="formData.radius"
                :min="0"
                :max="10000"
                placeholder="半径（可选）"
                clearable
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="坐标 X">
              <n-input-number
                v-model:value="formData.x"
                :min="-30000000"
                :max="30000000"
                placeholder="X（可选）"
                clearable
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="坐标 Y">
              <n-input-number
                v-model:value="formData.y"
                :min="-64"
                :max="320"
                placeholder="Y（可选）"
                clearable
              />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="坐标 Z">
              <n-input-number
                v-model:value="formData.z"
                :min="-30000000"
                :max="30000000"
                placeholder="Z（可选）"
                clearable
              />
            </n-form-item>
          </n-grid-item>
        </n-grid>

        <n-space class="action-bar" justify="center" :size="16">
          <n-button
            type="primary"
            :loading="previewLoading"
            @click="handlePreview"
            :disabled="executeLoading"
          >
            预览
          </n-button>
          <n-button
            type="warning"
            :loading="executeLoading"
            @click="handleExecute"
            :disabled="previewLoading"
          >
            执行回滚
          </n-button>
        </n-space>
      </n-form>
    </n-card>

    <n-card v-if="previewResult" :bordered="false" class="preview-card">
      <template #header>
        <div class="preview-header">
          <span>预览结果</span>
        </div>
      </template>

      <n-descriptions label-placement="left" :column="2" bordered size="small">
        <n-descriptions-item label="受影响位置数">
          {{ previewResult.affectedLocations }}
        </n-descriptions-item>
        <n-descriptions-item label="摘要">
          {{ previewResult.summary }}
        </n-descriptions-item>
      </n-descriptions>

      <n-data-table
        v-if="previewResult.records.length > 0"
        :columns="previewColumns"
        :data="previewResult.records"
        :bordered="false"
        :single-line="false"
        size="small"
        class="preview-table"
      />
    </n-card>

    <n-card v-if="executeResult" :bordered="false" class="execute-card">
      <template #header>
        <div class="execute-header">
          <span>执行结果</span>
        </div>
      </template>

      <n-result
        :type="executeResult.success ? 'success' : 'error'"
        :title="executeResult.success ? '回滚成功' : '回滚失败'"
        :description="executeResult.message"
      >
        <template #footer>
          <n-descriptions v-if="executeResult.affectedLocations" label-placement="left" :column="1" bordered size="small">
            <n-descriptions-item label="受影响位置数">
              {{ executeResult.affectedLocations }}
            </n-descriptions-item>
          </n-descriptions>
        </template>
      </n-result>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { format } from 'date-fns'
import { useMessage, useDialog } from 'naive-ui'
import type { DataTableColumn } from 'naive-ui'
import { api } from '../api/client'
import type { RollbackPreviewResponse, RollbackExecuteResponse } from '../api/client'

const message = useMessage()
const dialog = useDialog()

const previewLoading = ref(false)
const executeLoading = ref(false)
const previewResult = ref<RollbackPreviewResponse | null>(null)
const executeResult = ref<RollbackExecuteResponse | null>(null)

const timeUnitOptions = [
  { label: '秒', value: 'seconds' },
  { label: '分钟', value: 'minutes' },
  { label: '小时', value: 'hours' },
  { label: '天', value: 'days' },
]

const formData = reactive({
  timeAmount: 10,
  timeUnit: 'minutes',
  player: '',
  world: '',
  blockType: '',
  radius: null as number | null,
  x: null as number | null,
  y: null as number | null,
  z: null as number | null,
})

const previewColumns: DataTableColumn[] = [
  { title: '位置', key: 'location', width: 200, ellipsis: { tooltip: true } },
  { title: '旧方块', key: 'oldBlock', width: 140 },
  { title: '新方块', key: 'newBlock', width: 140 },
  { title: '玩家', key: 'player', width: 120 },
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
]

function getExecuteParams(confirm: boolean) {
  return {
    timeAmount: formData.timeAmount,
    timeUnit: formData.timeUnit,
    player: formData.player || undefined,
    world: formData.world || undefined,
    blockType: formData.blockType || undefined,
    radius: formData.radius ?? undefined,
    x: formData.x ?? undefined,
    y: formData.y ?? undefined,
    z: formData.z ?? undefined,
    confirm,
  }
}

async function handlePreview() {
  if (!formData.timeAmount) {
    message.warning('请填写时间量')
    return
  }

  previewLoading.value = true
  executeResult.value = null
  try {
    const params = {
      timeAmount: formData.timeAmount,
      timeUnit: formData.timeUnit,
      player: formData.player || undefined,
      world: formData.world || undefined,
      blockType: formData.blockType || undefined,
      radius: formData.radius ?? undefined,
      x: formData.x ?? undefined,
      y: formData.y ?? undefined,
      z: formData.z ?? undefined,
    }
    previewResult.value = await api.rollbackPreview(params)
  } catch (err: any) {
    message.error(err.message || '预览失败')
  } finally {
    previewLoading.value = false
  }
}

function handleExecute() {
  if (!formData.timeAmount) {
    message.warning('请填写时间量')
    return
  }

  dialog.warning({
    title: '确认回滚',
    content: '确定要执行回滚操作吗？此操作不可撤销。',
    positiveText: '确认执行',
    negativeText: '取消',
    onPositiveClick: async () => {
      executeLoading.value = true
      previewResult.value = null
      try {
        executeResult.value = await api.rollbackExecute(getExecuteParams(true))
        if (executeResult.value.success) {
          message.success('回滚执行成功')
        } else {
          message.error(executeResult.value.message || '回滚执行失败')
        }
      } catch (err: any) {
        message.error(err.message || '回滚执行失败')
      } finally {
        executeLoading.value = false
      }
    },
  })
}
</script>

<style scoped>
.rollback-page {
  max-width: 1000px;
  margin: 0 auto;
}

.rollback-card {
  background-color: #1a1a22;
  margin-bottom: 16px;
}

.rollback-header {
  font-size: 16px;
  font-weight: 600;
}

.action-bar {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.preview-card {
  background-color: #1a1a22;
  margin-bottom: 16px;
}

.preview-header {
  font-size: 15px;
  font-weight: 600;
}

.preview-table {
  margin-top: 16px;
}

.execute-card {
  background-color: #1a1a22;
  margin-bottom: 16px;
}

.execute-header {
  font-size: 15px;
  font-weight: 600;
}
</style>
