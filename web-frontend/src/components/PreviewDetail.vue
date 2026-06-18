<template>
  <n-scrollbar style="max-height: 400px">
    <n-list bordered>
      <n-list-item v-for="(detail, location, index) in preview" :key="index">
        <n-thing :title="location" :description="detail" />
      </n-list-item>
    </n-list>
  </n-scrollbar>
</template>

<script setup lang="ts">
defineProps<{
  preview: Record<string, string>
}>()
</script>
