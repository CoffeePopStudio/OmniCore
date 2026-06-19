<template>
  <div class="dashboard">
    <div class="page-header">
      <h1 class="text-large-title" style="margin:0">Dashboard</h1>
      <span class="text-label">Server overview &amp; quick actions</span>
    </div>

    <div class="stats-grid">
      <div class="glass-card stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-icon-wrapper" :class="'stat-' + card.color">
          <AppIcon :name="card.iconName" class="stat-icon" />
        </div>
        <div class="stat-body">
          <span class="text-caption stat-label">{{ card.label }}</span>
          <span class="stat-value" :class="'stat-' + card.color">{{ card.value }}</span>
          <span v-if="card.footer" class="text-caption stat-footer">{{ card.footer }}</span>
        </div>
      </div>
    </div>

    <div class="glass-card profile-card">
      <div class="profile-avatar" :style="{ background: avatarGradient }">
        <span class="profile-initial">{{ avatarLetter }}</span>
      </div>
      <div class="profile-info">
        <div class="profile-name-row">
          <span class="text-title-2 profile-username">{{ auth.username || 'Player' }}</span>
          <span class="status-dot" :class="health.status === 'ok' ? 'online' : 'offline'"></span>
          <span class="text-subhead status-label">{{ health.status === 'ok' ? 'Online' : 'Offline' }}</span>
        </div>
        <div class="profile-meta">
          <span class="text-label">UUID</span>
          <span class="profile-uuid">{{ auth.uuid || '—' }}</span>
        </div>
      </div>
    </div>

    <div class="actions-section">
      <span class="text-label" style="margin-bottom:12px;display:block">Quick Actions</span>
      <div class="actions-row">
        <router-link to="/query" class="glass-btn primary">
          <AppIcon name="search" />
          Query Records
        </router-link>
        <router-link to="/rollback" class="glass-btn amber">
          <AppIcon name="rollback" />
          Rollback
        </router-link>
        <router-link to="/logs" class="glass-btn purple">
          <AppIcon name="logs" />
          Server Logs
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useMessage } from 'naive-ui'
import AppIcon from '@/components/AppIcon.vue'
import { safeCount } from '@/utils/format'

const auth = useAuthStore()
const message = useMessage()
const health = ref<{ status: string; version?: string }>({ status: 'unknown', version: '' })
const blockCount = ref(0)
const containerCount = ref(0)
const inventoryCount = ref(0)

const avatarLetter = computed(() => {
  const name = auth.username || auth.uuid || 'P'
  return name.charAt(0).toUpperCase()
})

const avatarGradient = computed(() => {
  const colors = ['linear-gradient(135deg,#007AFF,#34C759)', 'linear-gradient(135deg,#AF52DE,#FF9500)', 'linear-gradient(135deg,#FF3B30,#007AFF)', 'linear-gradient(135deg,#34C759,#AF52DE)', 'linear-gradient(135deg,#FF9500,#FF3B30)']
  const idx = (auth.uuid || auth.username || 'P').charCodeAt(0) % colors.length
  return colors[idx]
})

const statCards = computed(() => [
  {
    label: 'Server Status',
    value: health.value.status,
    color: health.value.status === 'ok' ? 'green' : 'rose',
    iconName: 'server',
    footer: health.value.version ? `Version ${health.value.version}` : '',
  },
  {
    label: 'Blocks Logged',
    value: safeCount(blockCount.value),
    color: 'blue',
    iconName: 'blocks',
    footer: '',
  },
  {
    label: 'Container Logs',
    value: safeCount(containerCount.value),
    color: 'amber',
    iconName: 'container',
    footer: '',
  },
  {
    label: 'Inventory Logs',
    value: safeCount(inventoryCount.value),
    color: 'purple',
    iconName: 'inventory',
    footer: '',
  },
])

onMounted(() => {
  loadHealth()
  loadCounts()
})

async function loadHealth() {
  try {
    const data = await api.health()
    health.value = data
  } catch {
    health.value = { status: 'unreachable', version: '' }
  }
}

async function loadCounts() {
  try {
    const data = await api.stats()
    blockCount.value = data.blocks ?? 0
    containerCount.value = data.containers ?? 0
    inventoryCount.value = data.inventory ?? 0
  } catch (e: any) {
    message.error('Failed to load counts: ' + (e.message || 'Unknown error'))
  }
}
</script>

<style scoped>
.dashboard {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 28px;
}

.page-header .text-label {
  margin-top: 4px;
  display: block;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stat-icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.stat-icon {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-green .stat-icon { color: var(--lg-accent-green); }
.stat-blue .stat-icon  { color: var(--lg-accent); }
.stat-amber .stat-icon { color: var(--lg-accent-amber); }
.stat-purple .stat-icon { color: var(--lg-accent-purple); }
.stat-rose .stat-icon  { color: var(--lg-accent-rose); }

.stat-green .stat-icon-wrapper { background: rgba(52, 199, 89, 0.12); }
.stat-blue .stat-icon-wrapper  { background: rgba(0, 122, 255, 0.12); }
.stat-amber .stat-icon-wrapper { background: rgba(255, 149, 0, 0.12); }
.stat-purple .stat-icon-wrapper { background: rgba(175, 82, 222, 0.12); }
.stat-rose .stat-icon-wrapper  { background: rgba(255, 59, 48, 0.12); }

.stat-card:hover .stat-icon-wrapper {
  transform: scale(1.05);
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  color: var(--lg-text-secondary);
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: var(--lg-text);
  transition: color 0.3s ease;
}

.stat-green .stat-value { color: var(--lg-accent-green); }
.stat-blue .stat-value  { color: var(--lg-accent); }
.stat-amber .stat-value { color: var(--lg-accent-amber); }
.stat-purple .stat-value { color: var(--lg-accent-purple); }
.stat-rose .stat-value  { color: var(--lg-accent-rose); }

.stat-footer {
  color: var(--lg-text-tertiary);
  margin-top: 2px;
}

.profile-card {
  position: relative;
  padding: 22px 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 24px;
}

.profile-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
}

.profile-initial {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
  text-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.profile-username {
  color: var(--lg-text);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  position: relative;
}

.status-dot.online {
  background: var(--lg-accent-green);
  box-shadow: 0 0 8px var(--lg-accent-green), 0 0 20px rgba(52, 199, 89, 0.3);
}

.status-dot.offline {
  background: var(--lg-accent-rose);
  box-shadow: 0 0 8px var(--lg-accent-rose);
}

.status-label {
  color: var(--lg-text-secondary);
}

.profile-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-uuid {
  font-size: 13px;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', 'JetBrains Mono', monospace;
  color: var(--lg-text-tertiary);
  letter-spacing: 0.02em;
}

.actions-section {
  margin-bottom: 8px;
}

.actions-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 820px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 540px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .actions-row {
    flex-direction: column;
  }

  .actions-row .glass-btn {
    width: 100%;
  }
}
</style>
