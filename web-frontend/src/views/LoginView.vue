<template>
  <div class="login-wrapper">
    <n-card class="login-card" :bordered="false">
      <div class="login-header">
        <div class="login-logo">O</div>
        <h2>OnmiCore Web Panel</h2>
        <p class="login-subtitle">登录您的账户</p>
      </div>
      <n-form ref="formRef" :model="formData" :rules="rules" @submit.prevent="handleLogin">
        <n-form-item label="UUID" path="uuid">
          <n-input
            v-model:value="formData.uuid"
            placeholder="请输入您的 UUID"
            :disabled="loading"
          />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input
            v-model:value="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password-on="click"
            :disabled="loading"
            @keyup.enter="handleLogin"
          />
        </n-form-item>
        <n-form-item>
          <n-button
            type="primary"
            block
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </n-button>
        </n-form-item>
      </n-form>
      <div class="login-footer">
        <span>还没有账户？</span>
        <n-button text type="primary" @click="goRegister">立即注册</n-button>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormRules, FormInst } from 'naive-ui'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const authStore = useAuthStore()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)

const formData = reactive({
  uuid: '',
  password: '',
})

const rules: FormRules = {
  uuid: [
    { required: true, message: '请输入 UUID', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ],
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.login(formData.uuid, formData.password)
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (err: any) {
    message.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<style scoped>
.login-wrapper {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0f14 0%, #1a1a2e 50%, #0f0f14 100%);
}

.login-card {
  width: 400px;
  padding: 12px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #7c3aed, #3b82f6);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 24px;
  margin: 0 auto 16px;
}

.login-header h2 {
  color: #e0e0e0;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.login-subtitle {
  color: #888;
  font-size: 14px;
}

.login-footer {
  text-align: center;
  color: #888;
  font-size: 13px;
}
</style>
