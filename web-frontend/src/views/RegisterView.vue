<template>
  <div style="max-width: 400px; margin: 80px auto;">
    <n-card title="注册账号">
      <n-alert v-if="errorMessage" :title="errorMessage" type="error" closable @close="errorMessage = ''" style="margin-bottom: 16px;" />
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-form-item path="username" label="用户名">
          <n-input v-model:value="form.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item path="password" label="密码">
          <n-input v-model:value="form.password" type="password" placeholder="请输入密码" show-password-on="click" />
        </n-form-item>
        <n-form-item path="confirmPassword" label="确认密码">
          <n-input v-model:value="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password-on="click" />
        </n-form-item>
        <n-space vertical style="width: 100%;">
          <n-button type="primary" block :loading="loading" @click="handleRegister">注册</n-button>
          <n-button block @click="router.push('/login')">返回登录</n-button>
        </n-space>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInst, FormRules } from 'naive-ui'
import { register } from '../api/client'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInst | null>(null)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})
const loading = ref(false)
const errorMessage = ref('')

const rules: FormRules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' },
  confirmPassword: {
    required: true,
    message: '请确认密码',
    trigger: 'blur',
    validator: (_rule, value) => {
      if (value !== form.password) {
        return new Error('两次输入的密码不一致')
      }
      return true
    },
  },
}

async function handleRegister() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const uuid = (route.query.uuid as string) || ''
    const playerName = (route.query.playerName as string) || form.username
    const res = await register(uuid, form.username, form.password, playerName)
    localStorage.setItem('token', res.data.token)
    router.push('/dashboard')
  } catch (err: any) {
    errorMessage.value = '注册失败: ' + (err.response?.data?.message || err.message)
  } finally {
    loading.value = false
  }
}
</script>
