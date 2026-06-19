<template>
  <component :is="iconComponent" v-if="iconComponent" :aria-label="ariaLabel" aria-hidden="!ariaLabel" />
  <span v-else-if="name === 'blocks'" class="app-icon-custom" role="img" :aria-label="ariaLabel || 'blocks icon'" v-html="iconBlocks"></span>
  <span v-else-if="name === 'container'" class="app-icon-custom" role="img" :aria-label="ariaLabel || 'container icon'" v-html="iconContainer"></span>
  <span v-else-if="name === 'inventory'" class="app-icon-custom" role="img" :aria-label="ariaLabel || 'inventory icon'" v-html="iconInventory"></span>
  <span v-else-if="name === 'rollback'" class="app-icon-custom" role="img" :aria-label="ariaLabel || 'rollback icon'" v-html="iconRollback"></span>
  <span v-else-if="name === 'search'" class="app-icon-custom" role="img" :aria-label="ariaLabel || 'search icon'" v-html="iconSearch"></span>
  <span v-else-if="name === 'refresh'" class="app-icon-custom" role="img" :aria-label="ariaLabel || 'refresh icon'" v-html="iconRefresh"></span>
  <span v-else-if="name === 'check'" class="app-icon-custom" role="img" :aria-label="ariaLabel || 'check icon'" v-html="iconCheck"></span>
  <span v-else-if="name === 'logs'" class="app-icon-custom" role="img" :aria-label="ariaLabel || 'logs icon'" v-html="iconLogs"></span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Server20Filled, Cube20Filled, Box20Filled, Person20Filled } from '@vicons/fluent'

const props = defineProps<{
  name: string
  size?: number
  ariaLabel?: string
}>()

const size = computed(() => props.size || 20)

const iconMap: Record<string, any> = {
  server: Server20Filled,
  blocks: null,
  container: null,
  inventory: null,
  rollback: null,
  search: null,
  refresh: null,
  check: null,
  logs: null,
  person: Person20Filled,
  cube: Cube20Filled,
  box: Box20Filled,
}

const iconComponent = computed(() => iconMap[props.name] || null)

const svgAttrs = `width="${size.value}" height="${size.value}"`

const iconBlocks = `<svg ${svgAttrs} viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>`
const iconContainer = `<svg ${svgAttrs} viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>`
const iconInventory = `<svg ${svgAttrs} viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`
const iconRollback = `<svg ${svgAttrs} viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>`
const iconSearch = `<svg ${svgAttrs} viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>`
const iconRefresh = `<svg ${svgAttrs} viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>`
const iconCheck = `<svg ${svgAttrs} viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>`
const iconLogs = `<svg ${svgAttrs} viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>`
</script>

<style scoped>
.app-icon-custom {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
}
</style>
