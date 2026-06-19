<template>
  <span class="loading-spinner" :style="{ width: size + 'px', height: size + 'px' }"></span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  size?: number
}>(), {
  size: 16,
})
</script>

<style scoped>
.loading-spinner {
  display: inline-block;
  border: 2px solid var(--lg-text-secondary);
  border-top-color: var(--lg-text);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
