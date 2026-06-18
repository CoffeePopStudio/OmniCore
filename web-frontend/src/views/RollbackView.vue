<template>
  <div>
    <n-h2>Rollback</n-h2>
    <n-grid :cols="2" :x-gap="16">
      <n-gi>
        <n-card title="Rollback Parameters">
          <n-form @submit.prevent="doPreview">
            <n-form-item label="Time">
              <n-input v-model:value="params.time" placeholder="e.g. 5m, 1h, 7d" />
            </n-form-item>
            <n-form-item label="Player">
              <n-input v-model:value="params.player" placeholder="Filter by player (optional)" />
            </n-form-item>
            <n-form-item label="World">
              <n-input v-model:value="params.world" placeholder="Filter by world (optional)" />
            </n-form-item>
            <n-form-item label="Block Type">
              <n-input v-model:value="params.blockType" placeholder="Filter by block type (optional)" />
            </n-form-item>
            <n-collapse>
              <n-collapse-item title="Radius Filter">
                <n-form-item label="Radius">
                  <n-input-number v-model:value="params.radius" :min="0" placeholder="0 = no radius" />
                </n-form-item>
                <n-grid :cols="3" :x-gap="8">
                  <n-gi>
                    <n-form-item label="X">
                      <n-input-number v-model:value="params.x" />
                    </n-form-item>
                  </n-gi>
                  <n-gi>
                    <n-form-item label="Y">
                      <n-input-number v-model:value="params.y" />
                    </n-form-item>
                  </n-gi>
                  <n-gi>
                    <n-form-item label="Z">
                      <n-input-number v-model:value="params.z" />
                    </n-form-item>
                  </n-gi>
                </n-grid>
              </n-collapse-item>
            </n-collapse>
            <n-space style="margin-top: 16px">
              <n-button type="primary" attr-type="submit" :loading="previewLoading">
                Preview Rollback
              </n-button>
            </n-space>
          </n-form>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="Preview">
          <template v-if="previewResult">
            <n-statistic title="Affected Locations" :value="previewResult.count" />
            <n-divider />
            <n-card v-if="!executing" title="Preview Details" size="small">
              <preview-detail :preview="previewResult.preview" />
              <n-space style="margin-top: 16px">
                <n-button type="warning" @click="executeRollback" :loading="executing">
                  Execute Rollback
                </n-button>
              </n-space>
            </n-card>
            <n-card v-else title="Progress">
              <n-progress type="line" :percentage="progress" :indicator-placement="'inside'" />
              <n-text v-if="executionStatus">{{ executionStatus }}</n-text>
            </n-card>
          </template>
          <n-empty v-else description="Run a preview to see results" />
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { api, type RollbackPreviewResponse } from '@/api/client'
import { useMessage } from 'naive-ui'

const message = useMessage()
const previewLoading = ref(false)
const executing = ref(false)
const progress = ref(0)
const executionStatus = ref('')
const previewResult = ref<RollbackPreviewResponse | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const params = reactive({
  time: '5m',
  player: '',
  world: '',
  blockType: '',
  radius: 0,
  x: 0,
  y: 0,
  z: 0,
})

async function doPreview() {
  previewLoading.value = true
  try {
    const queryParams: Record<string, any> = { time: params.time }
    if (params.player) queryParams.player = params.player
    if (params.world) queryParams.world = params.world
    if (params.blockType) queryParams.block_type = params.blockType
    if (params.radius && params.radius > 0) {
      queryParams.radius = params.radius
      queryParams.x = params.x
      queryParams.y = params.y
      queryParams.z = params.z
    }
    const result = await api.rollbackPreview(queryParams)
    previewResult.value = result
    if (result.count === 0) {
      message.warning('No changes to rollback in the given time range')
    }
  } catch (e: any) {
    message.error(e.message || 'Preview failed')
  } finally {
    previewLoading.value = false
  }
}

async function executeRollback() {
  if (!previewResult.value || previewResult.value.count === 0) {
    message.warning('Nothing to execute')
    return
  }

  executing.value = true
  progress.value = 0
  executionStatus.value = 'Rollback started...'

  try {
    const queryParams: Record<string, any> = { time: params.time, confirm: 'true' }
    if (params.player) queryParams.player = params.player
    if (params.world) queryParams.world = params.world
    if (params.blockType) queryParams.block_type = params.blockType
    if (params.radius && params.radius > 0) {
      queryParams.radius = params.radius
      queryParams.x = params.x
      queryParams.y = params.y
      queryParams.z = params.z
    }

    const result = await api.rollbackExecute(queryParams)
    executionStatus.value = 'Rollback executed successfully'
    progress.value = 100
    message.success('Rollback complete!')
  } catch (e: any) {
    message.error(e.message || 'Rollback failed')
    executionStatus.value = `Failed: ${e.message}`
  } finally {
    executing.value = false
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }
}
</script>
