<template>
  <div style="max-width: 400px; margin: 80px auto;">
    <n-card title="OnmiCore 绑定 / 登录">
      <n-alert v-if="bindMessage" :title="bindMessage" :type="bindStatus" closable @close="bindMessage = ''" style="margin-bottom: 16px;" />
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-form-item path="username" label="用户名">
          <n-input v-model:value="form.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item path="password" label="密码">
          <n-input v-model:value="form.password" type="password" placeholder="请输入密码" show-password-on="click" />
        </n-form-item>
        <n-space vertical style="width: 100%;">
          <n-button type="primary" block :loading="loading" @click="handleLogin">登录</n-button>
          <n-button v-if="showRegister" block @click="handleGoRegister">注册账号</n-button>
        </n-space>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInst, FormRules } from 'naive-ui'
import { bind, login } from '../api/client'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInst | null>(null)
const form = ref({ username: '', password: '' })
const loading = ref(false)
const showRegister = ref(false)
const bindMessage = ref('')
const bindStatus = ref<'success' | 'info' | 'warning' | 'error'>('info')

const uuid = ref('')

const rules: FormRules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' },
}

async function handleBind(uuidParam: string) {
  uuid.value = uuidParam
  try {
    const res = await bind(uuidParam)
    if (res.data.token) {
      localStorage.setItem('token', res.data.token)
      router.push('/dashboard')
    }
  } catch (err: any) {
    if (err.response?.data?.status === 'not_registered') {
      showRegister.value = true
      bindMessage.value = '该 UUID 未注册，请点击注册按钮完成注册'
      bindStatus.value = 'warning'
    } else {
      bindMessage.value = '绑定失败: ' + (err.response?.data?.message || err.message)
      bindStatus.value = 'error'
    }
  }
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await login(form.value.username, form.value.password)
    localStorage.setItem('token', res.data.token)
    router.push('/dashboard')
  } catch (err: any) {
    bindMessage.value = '登录失败: ' + (err.response?.data?.message || err.message)
    bindStatus.value = 'error'
  } finally {
    loading.value = false
  }
}

function handleGoRegister() {
  const query: Record<string, string> = {}
  if (uuid.value) query.uuid = uuid.value
  router.push({ name: 'Register', query })
}

onMounted(() => {
  const uuidParam = route.query.uuid as string | undefined
  if (uuidParam) {
    handleBind(uuidParam)
  }
})
</script>
