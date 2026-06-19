import { ref, onUnmounted } from 'vue'
import { api } from '@/api/client'
import type { RollbackFilters } from '@/types'

export function useRollback() {
  const loading = ref(false)
  const executing = ref(false)
  const previewData = ref<Record<string, string> | null>(null)
  const previewCount = ref(0)
  const previewText = ref('')
  const ticket = ref<string | null>(null)
  const progress = ref(0)
  let progressInterval: ReturnType<typeof setInterval> | null = null

  function buildParams(timeAmount: string, filters: RollbackFilters): Record<string, any> {
    const params: Record<string, any> = { time: timeAmount }
    if (filters.player) params.player = filters.player
    if (filters.world) params.world = filters.world
    if (filters.blockType) params.block_type = filters.blockType
    if (filters.radius > 0 && filters.center) {
      const parts = filters.center.split(',').map(s => parseInt(s.trim()))
      if (parts.length === 3) {
        params.radius = filters.radius
        params.x = parts[0]
        params.y = parts[1]
        params.z = parts[2]
      }
    }
    return params
  }

  async function handlePreview(timeAmount: string, filters: RollbackFilters): Promise<string | null> {
    loading.value = true
    previewData.value = null
    previewCount.value = 0
    try {
      const params = buildParams(timeAmount, filters)
      const result = await api.rollbackPreview(params)
      previewData.value = result.preview
      previewCount.value = result.count
      const lines = Object.entries(result.preview).map(([k, v]) => `${k}: ${v}`)
      previewText.value = lines.join('\n')
      return null
    } catch (e: any) {
      return e.message || 'Preview failed'
    }
  }

  async function handleExecute(timeAmount: string, filters: RollbackFilters): Promise<string | null> {
    executing.value = true
    try {
      const params = buildParams(timeAmount, filters)
      const result = await api.rollbackExecute(params)
      ticket.value = result.ticket || ''
      progress.value = 0
      previewData.value = null

      stopPolling()
      progressInterval = setInterval(async () => {
        try {
          const p = await api.rollbackProgress(ticket.value!)
          progress.value = p.progress
          if (p.progress >= 100) {
            stopPolling()
          }
        } catch {
          stopPolling()
        }
      }, 2000)

      return null
    } catch (e: any) {
      return e.message || 'Rollback execution failed'
    } finally {
      executing.value = false
    }
  }

  function stopPolling() {
    if (progressInterval) {
      clearInterval(progressInterval)
      progressInterval = null
    }
  }

  function reset() {
    previewData.value = null
    previewCount.value = 0
    previewText.value = ''
    ticket.value = null
    progress.value = 0
    stopPolling()
  }

  onUnmounted(() => {
    stopPolling()
  })

  return {
    loading,
    executing,
    previewData,
    previewCount,
    previewText,
    ticket,
    progress,
    handlePreview,
    handleExecute,
    reset,
    buildParams,
  }
}
