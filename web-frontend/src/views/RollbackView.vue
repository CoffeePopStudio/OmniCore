<template>
  <div class="rollback-page">
    <div class="page-header">
      <h2 class="text-title-1">Rollback</h2>
      <span class="text-subhead page-subtitle">Preview and execute block rollbacks</span>
    </div>

    <div class="glass-card filter-card">
      <span class="text-label filter-label">Rollback Parameters</span>
      <n-form @submit.prevent="handlePreviewClick">
        <div class="filter-grid">
          <n-select v-model:value="timeAmount" :options="timeOptions" placeholder="Time to rollback" />
          <n-input v-model:value="filters.player" placeholder="Player name (optional)" />
          <n-input v-model:value="filters.world" placeholder="World (optional)" />
          <n-input v-model:value="filters.blockType" placeholder="Block type (optional)" />
          <n-input-number v-model:value="filters.radius" placeholder="Radius" :min="0" style="width:100%" />
          <n-input v-model:value="filters.center" placeholder="x,y,z (with radius)" />
        </div>
        <div class="filter-actions">
          <button class="glass-btn primary" type="submit" :disabled="isPreviewDisabled">
            <AppIcon name="rollback" />
            <span v-if="loading">Loading…</span>
            <span v-else>Preview Rollback</span>
          </button>
          <button class="glass-btn" type="button" @click="resetFilters">Reset</button>
        </div>
      </n-form>
    </div>

    <div v-if="hasPreview" class="glass-card preview-card">
      <div class="preview-header">
        <span class="text-title-2">Rollback Preview</span>
        <span class="preview-badge">{{ previewCount }} affected locations</span>
      </div>
      <n-input type="textarea" :value="previewText" autosize :minrows="5" :maxrows="15" readonly />
      <div class="preview-actions">
        <button class="glass-btn amber" :disabled="executing" @click="handleExecuteClick">
          <AppIcon name="check" />
          <span v-if="executing">Executing…</span>
          <span v-else>Confirm &amp; Execute Rollback</span>
        </button>
        <button class="glass-btn" @click="cancelPreview">Cancel</button>
      </div>
    </div>

    <div v-if="hasTicket" class="glass-card progress-card">
      <div class="progress-header">
        <span class="text-title-2">Execution Progress</span>
        <span class="text-subhead" :class="progress >= 100 ? 'text-success' : 'text-info'">
          {{ progress >= 100 ? 'Completed' : 'In progress' }}
        </span>
      </div>
      <n-progress type="line" :percentage="progress" :indicator-placement="'inside'" />
      <div v-if="progress >= 100" class="success-message">
        <AppIcon name="check" />
        <span>Rollback completed successfully</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useMessage } from 'naive-ui'
import { useRollback } from '@/composables/useRollback'
import type { RollbackFilters } from '@/types'
import AppIcon from '@/components/AppIcon.vue'

const message = useMessage()
const rollback = useRollback()

const loading = rollback.loading
const executing = rollback.executing
const hasPreview = computed(() => rollback.previewData.value !== null)
const previewCount = rollback.previewCount
const previewText = rollback.previewText
const hasTicket = computed(() => rollback.ticket.value !== null)
const progress = rollback.progress

const isPreviewDisabled = computed(() => loading.value || hasPreview.value)

const timeAmount = ref('30m')
const timeOptions = [
  { label: '5 minutes', value: '5m' },
  { label: '10 minutes', value: '10m' },
  { label: '30 minutes', value: '30m' },
  { label: '1 hour', value: '1h' },
  { label: '3 hours', value: '3h' },
  { label: '6 hours', value: '6h' },
  { label: '12 hours', value: '12h' },
  { label: '1 day', value: '1d' },
  { label: '3 days', value: '3d' },
  { label: '7 days', value: '7d' },
]

const filters = reactive<RollbackFilters>({
  player: '',
  world: '',
  blockType: '',
  radius: 0,
  center: '',
})

async function handlePreviewClick() {
  if (filters.radius > 0 && filters.center) {
    const parts = filters.center.split(',').map(s => parseInt(s.trim()))
    if (parts.length === 3 && !filters.world) {
      message.warning('Please specify a world when using radius')
      return
    }
  }
  const error = await rollback.handlePreview(timeAmount.value, filters)
  if (error) message.error(error)
}

async function handleExecuteClick() {
  const error = await rollback.handleExecute(timeAmount.value, filters)
  if (error) message.error(error)
  else message.success('Rollback started')
}

function cancelPreview() {
  rollback.reset()
}

function resetFilters() {
  filters.player = ''
  filters.world = ''
  filters.blockType = ''
  filters.radius = 0
  filters.center = ''
  rollback.reset()
}
</script>

<style scoped>
.rollback-page {
  max-width: 960px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-subtitle {
  display: block;
  color: var(--lg-text-secondary);
  margin-top: 4px;
}

.filter-card {
  padding: 24px;
  margin-bottom: 16px;
  position: relative;
}

.filter-label {
  display: block;
  margin-bottom: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.filter-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--lg-glass-border);
}

.filter-actions .glass-btn {
  font-size: 14px;
  padding: 10px 24px;
}

.preview-card {
  padding: 24px;
  margin-bottom: 16px;
  position: relative;
  animation: cardSlideIn 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.preview-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  background: var(--lg-accent);
  color: #fff;
  letter-spacing: -0.01em;
}

.preview-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--lg-glass-border);
}

.preview-actions .glass-btn {
  font-size: 14px;
  padding: 10px 24px;
}

.progress-card {
  padding: 24px;
  position: relative;
  animation: cardSlideIn 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.text-success {
  color: var(--lg-accent-green);
}

.text-info {
  color: var(--lg-accent);
}

.success-message {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--lg-glass-border);
  font-size: 14px;
  font-weight: 500;
  color: var(--lg-accent-green);
  animation: fadeIn 0.4s ease;
}

.glass-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

@keyframes cardSlideIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 768px) {
  .filter-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .preview-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .filter-actions,
  .preview-actions {
    flex-direction: column;
  }

  .filter-actions .glass-btn,
  .preview-actions .glass-btn {
    width: 100%;
  }
}
</style>
