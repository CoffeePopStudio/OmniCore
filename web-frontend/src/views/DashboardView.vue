<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">Dashboard</h2>
      <span class="page-subtitle">Server overview &amp; quick actions</span>
    </div>

    <!-- Data Cards Row -->
    <div class="stats-grid">
      <div class="stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-icon" :class="card.color" v-html="card.icon"></div>
        <div class="stat-body">
          <span class="stat-label">{{ card.label }}</span>
          <span class="stat-value" :class="card.color">{{ card.value }}</span>
          <span v-if="card.footer" class="stat-footer">{{ card.footer }}</span>
        </div>
      </div>
    </div>

    <!-- User Profile Card -->
    <div class="profile-card">
      <div class="profile-avatar">
        <span class="profile-initial">{{ avatarLetter }}</span>
      </div>
      <div class="profile-info">
        <div class="profile-name-row">
          <span class="profile-username">{{ auth.username || 'Player' }}</span>
          <span class="status-dot" :class="health.status === 'ok' ? 'online' : 'offline'"></span>
          <span class="status-label">{{ health.status === 'ok' ? 'Online' : 'Offline' }}</span>
        </div>
        <div class="profile-meta">
          <span class="profile-uuid-label">UUID</span>
          <span class="profile-uuid-value">{{ auth.uuid || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="actions-card">
      <h3 class="actions-title">Quick Actions</h3>
      <div class="actions-row">
        <router-link to="/query" class="action-btn primary">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          Query Records
        </router-link>
        <router-link to="/rollback" class="action-btn warning">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
          Rollback
        </router-link>
        <router-link to="/logs" class="action-btn secondary">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
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

const iconServer = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="2" width="20" height="8" rx="2" ry="2"/><rect x="2" y="14" width="20" height="8" rx="2" ry="2"/><line x1="6" y1="6" x2="6.01" y2="6"/><line x1="6" y1="18" x2="6.01" y2="18"/></svg>`
const iconBlocks = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>`
const iconContainer = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>`
const iconInventory = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`

const statCards = computed(() => [
  {
    label: 'Server Status',
    value: health.value.status,
    color: health.value.status === 'ok' ? 'green' : 'rose',
    icon: iconServer,
    footer: health.value.version ? `Version ${health.value.version}` : '',
  },
  {
    label: 'Blocks Logged',
    value: blockCount.value.toLocaleString(),
    color: 'blue',
    icon: iconBlocks,
    footer: '',
  },
  {
    label: 'Container Logs',
    value: containerCount.value.toLocaleString(),
    color: 'amber',
    icon: iconContainer,
    footer: '',
  },
  {
    label: 'Inventory Logs',
    value: inventoryCount.value.toLocaleString(),
    color: 'purple',
    icon: iconInventory,
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
    const [blocks, containers, inventory] = await Promise.all([
      api.statsBlocks(),
      api.statsContainers(),
      api.statsInventory(),
    ])
    blockCount.value = blocks.count
    containerCount.value = containers.count
    inventoryCount.value = inventory.count
  } catch (e: any) {
    message.error('Failed to load counts: ' + (e.message || 'Unknown error'))
  }
}
</script>

<style scoped>
.dashboard {
  max-width: 960px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  line-height: 1.3;
}

.page-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
  display: block;
}

/* ── Stats Grid ── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  padding: 18px 18px 16px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  box-shadow: var(--shadow-card);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0,0,0,0.35);
  border-color: var(--accent-blue);
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--bg-card-hover);
  color: var(--text-muted);
  transition: color 0.2s;
}

.stat-card:hover .stat-icon {
  color: var(--accent-blue);
}

.stat-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  transition: color 0.2s;
}

.stat-value.green { color: var(--accent-green); }
.stat-value.blue  { color: var(--accent-blue); }
.stat-value.amber { color: var(--accent-amber); }
.stat-value.purple { color: var(--accent-purple); }
.stat-value.rose  { color: var(--accent-rose); }

.stat-footer {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* ── Profile Card ── */
.profile-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  box-shadow: var(--shadow-card);
  margin-bottom: 16px;
}

.profile-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--avatar-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-initial {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.profile-username {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.online {
  background: var(--accent-green);
  box-shadow: 0 0 6px var(--accent-green);
}

.status-dot.offline {
  background: var(--accent-rose);
  box-shadow: 0 0 6px var(--accent-rose);
}

.status-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
}

.profile-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.profile-uuid-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.profile-uuid-value {
  font-size: 12px;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  color: var(--text-secondary);
}

/* ── Quick Actions ── */
.actions-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  padding: 18px 24px 22px;
  box-shadow: var(--shadow-card);
}

.actions-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 14px;
}

.actions-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s ease;
  border: 1px solid var(--border-card);
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.action-btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.15);
}

.action-btn.primary {
  background: var(--accent-blue);
  color: #fff;
  border-color: var(--accent-blue);
}

.action-btn.warning {
  background: var(--accent-amber);
  color: #1a1a1a;
  border-color: var(--accent-amber);
}

.action-btn.secondary {
  background: var(--accent-purple);
  color: #fff;
  border-color: var(--accent-purple);
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
