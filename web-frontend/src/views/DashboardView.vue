<template>
  <div>
    <n-h2>仪表盘</n-h2>
    <n-grid :cols="3" x-gap="16" y-gap="16">
      <n-grid-item>
        <n-card title="服务器状态" size="small">
          <n-description>
            <n-thing description="插件运行正常" />
          </n-description>
        </n-card>
      </n-grid-item>
      <n-grid-item>
        <n-card title="快速查询" size="small">
          <n-form label-placement="top">
            <n-form-item label="玩家名">
              <n-input v-model:value="quickQuery.player" placeholder="输入玩家名" />
            </n-form-item>
            <n-form-item label="时间范围">
              <n-select v-model:value="quickQuery.time" :options="timeOptions" />
            </n-form-item>
            <n-button type="primary" block @click="goQuery">查询</n-button>
          </n-form>
        </n-card>
      </n-grid-item>
      <n-grid-item>
        <n-card title="快速回滚" size="small">
          <n-form label-placement="top">
            <n-form-item label="玩家名">
              <n-input v-model:value="quickRollback.player" placeholder="输入玩家名" />
            </n-form-item>
            <n-form-item label="时间范围">
              <n-select v-model:value="quickRollback.time" :options="timeOptions" />
            </n-form-item>
            <n-button type="warning" block @click="goRollback">回滚</n-button>
          </n-form>
        </n-card>
      </n-grid-item>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const timeOptions = [
  { label: '30 分钟', value: '30m' },
  { label: '1 小时', value: '1h' },
  { label: '6 小时', value: '6h' },
  { label: '1 天', value: '1d' },
]

const quickQuery = reactive({ player: '', time: '30m' })
const quickRollback = reactive({ player: '', time: '30m' })

function goQuery() {
  router.push({
    name: 'Query',
    query: { player: quickQuery.player, time: quickQuery.time },
  })
}

function goRollback() {
  router.push({
    name: 'Rollback',
    query: { player: quickRollback.player, time: quickRollback.time },
  })
}
</script>
