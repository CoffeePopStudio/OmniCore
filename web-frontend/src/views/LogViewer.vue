<template>
  <div class="log-viewer">
    <div class="glass-card log-card">
      <div class="log-header">
        <span class="text-title-1">Server Logs</span>
        <div class="log-header-actions">
          <n-select
            v-model:value="linesCount"
            :options="linesOptions"
            size="small"
            class="log-lines-select"
          />
          <button class="glass-btn" :class="{ 'is-loading': loading }" @click="loadLogs">
            <LoadingSpinner v-if="loading" />
            <AppIcon v-else name="refresh" />
            Refresh
          </button>
        </div>
      </div>

      <div class="log-meta">
        <span class="text-caption">Total lines: {{ totalLines }} &middot; Showing last {{ linesCount }}</span>
      </div>

      <div class="log-textarea-wrapper">
        <div class="log-textarea" :class="{ 'log-empty': logContent === '(empty)' }">
          <pre>{{ logContent }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/api/client'
import { useMessage } from 'naive-ui'
import AppIcon from '@/components/AppIcon.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const message = useMessage()
const loading = ref(false)
const logContent = ref('Loading...')
const totalLines = ref(0)
const linesCount = ref(200)
const linesOptions = [
  { label: '50 lines', value: 50 },
  { label: '100 lines', value: 100 },
  { label: '200 lines', value: 200 },
  { label: '500 lines', value: 500 },
  { label: '1000 lines', value: 1000 },
]

onMounted(() => {
  loadLogs()
})

async function loadLogs() {
  loading.value = true
  try {
    const result = await api.logsPlugin(linesCount.value)
    logContent.value = result.content || '(empty)'
    totalLines.value = result.total_lines
  } catch (e: any) {
    logContent.value = 'Failed to load logs: ' + (e.message || 'Unknown error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.log-viewer {
  max-width: 960px;
  margin: 0 auto;
}

.log-card {
  padding: 28px 32px;
  position: relative;
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.log-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.log-lines-select {
  width: 130px;
}

.log-meta {
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--lg-glass-border);
}

.log-textarea-wrapper {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.2);
}

[data-theme="light"] .log-textarea-wrapper {
  background: rgba(0, 0, 0, 0.03);
}

.log-textarea {
  padding: 16px;
  overflow-x: auto;
}

.log-textarea pre {
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', 'JetBrains Mono', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--lg-text-secondary);
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}

.log-empty pre {
  color: var(--lg-text-tertiary);
  font-style: italic;
}

.loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid var(--lg-text-secondary);
  border-top-color: var(--lg-text);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.is-loading {
  opacity: 0.7;
  pointer-events: none;
}
</style>
