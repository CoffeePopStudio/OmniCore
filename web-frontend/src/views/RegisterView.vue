<template>
  <div class="register-wrapper">
    <n-card class="register-card" :bordered="false">
      <div class="register-header">
        <div class="register-logo">O</div>
        <h2>创建账户</h2>
        <p class="register-subtitle">注册 OnmiCore Web Panel 账户</p>
      </div>
      <n-form ref="formRef" :model="formData" :rules="rules" @submit.prevent="handleRegister">
        <n-form-item v-if="!isBind" label="UUID" path="uuid">
          <n-input
            v-model:value="formData.uuid"
            placeholder="请输入您的 UUID"
            :disabled="loading"
          />
        </n-form-item>
        <n-form-item label="用户名" path="username">
          <n-input
            v-model:value="formData.username"
            placeholder="请输入用户名"
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
          />
        </n-form-item>
        <n-form-item label="确认密码" path="confirmPassword">
          <n-input
            v-model:value="formData.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password-on="click"
            :disabled="loading"
          />
        </n-form-item>
        <n-form-item>
          <n-button
            type="primary"
            block
            :loading="loading"
            @click="handleRegister"
          >
            {{ isBind ? '绑定账户' : '注册' }}
          </n-button>
        </n-form-item>
      </n-form>
      <div class="register-footer">
        <span>已有账户？</span>
        <n-button text type="primary" @click="goLogin">立即登录</n-button>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
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

const isBind = computed(() => !!route.query.bind_token)
const bindToken = computed(() => (route.query.bind_token as string) || '')

const formData = reactive({
  uuid: '',
  username: '',
  password: '',
  confirmPassword: '',
})

const rules: FormRules = {
  uuid: [
    { required: true, message: '请输入 UUID', trigger: 'blur' },
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度为 3-32 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string) => {
        return value === formData.password || '两次输入的密码不一致'
      },
      trigger: 'blur',
    },
  ],
}

async function handleRegister() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (isBind.value) {
      await authStore.bind(bindToken.value, formData.username, formData.password)
      message.success('绑定成功')
    } else {
      await authStore.register(formData.uuid, formData.username, formData.password)
      message.success('注册成功')
    }
    router.push('/')
  } catch (err: any) {
    message.error(err.message || '注册失败')
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.register-wrapper {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0f14 0%, #1a1a2e 50%, #0f0f14 100%);
}

.register-card {
  width: 420px;
  padding: 12px;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-logo {
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

.register-header h2 {
  color: #e0e0e0;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.register-subtitle {
  color: #888;
  font-size: 14px;
}

.register-footer {
  text-align: center;
  color: #888;
  font-size: 13px;
}
</style>
