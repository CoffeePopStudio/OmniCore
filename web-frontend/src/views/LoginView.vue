<template>
  <div class="login-page">
    <div class="login-container">
      <div class="glass-card login-card">
        <div class="login-header">
          <div class="login-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
            </svg>
          </div>
          <h1 class="text-large-title">Sign In</h1>
          <p class="login-subtitle">Access your OnmiCore panel</p>
        </div>

        <div v-if="hasBindToken" class="glass-alert">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>
          </svg>
          <span>A bind token was detected. If you haven't registered yet, please go to <router-link to="/register">Register</router-link> first.</span>
        </div>

        <n-form ref="formRef" :model="formData" :rules="rules" @submit.prevent="handleLogin">
          <n-form-item path="uuid">
            <n-input v-model:value="formData.uuid" placeholder="Minecraft UUID" autocomplete="username" />
          </n-form-item>
          <n-form-item path="password">
            <n-input v-model:value="formData.password" type="password" show-password-on="click" placeholder="Password" autocomplete="current-password" />
          </n-form-item>
          <n-button type="primary" attr-type="submit" :loading="loading" block size="large">
            Sign In
          </n-button>
        </n-form>

        <div class="login-footer">
          <span class="footer-text">Don't have an account?</span>
          <router-link to="/register" class="footer-link">Register</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMessage } from 'naive-ui'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const message = useMessage()
const loading = ref(false)
const hasBindToken = ref(false)

const formData = reactive({
  uuid: '',
  password: '',
})

const rules = {
  uuid: [{ required: true, message: 'Please enter your UUID' }],
  password: [{ required: true, message: 'Please enter your password' }],
}

onMounted(() => {
  const bindToken = route.query.bind_token as string
  const uuidParam = route.query.uuid as string
  if (bindToken) {
    hasBindToken.value = true
  }
  if (uuidParam) {
    formData.uuid = uuidParam
  }
})

async function handleLogin() {
  try {
    loading.value = true
    await auth.login(formData.uuid, formData.password)
    message.success('Login successful')
    router.push('/dashboard')
  } catch (e: any) {
    message.error(e.message || 'Login failed')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-container {
  width: 100%;
  max-width: 400px;
  margin-top: -5vh;
}

.login-card {
  padding: 40px 32px 36px;
  position: relative;
  overflow: hidden;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: var(--lg-accent);
  color: #fff;
  margin-bottom: 16px;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.25);
}

.login-subtitle {
  font-size: 14px;
  color: var(--lg-text-secondary);
  margin-top: 6px;
}

.glass-alert {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 24px;
  border-radius: 12px;
  background: rgba(90, 200, 250, 0.1);
  border: 1px solid rgba(90, 200, 250, 0.2);
  color: var(--lg-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  backdrop-filter: var(--lg-blur);
  -webkit-backdrop-filter: var(--lg-blur);
}

.glass-alert svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--lg-accent);
}

.glass-alert a {
  color: var(--lg-accent);
  text-decoration: none;
  font-weight: 500;
}

.glass-alert a:hover {
  text-decoration: underline;
}

:deep(.n-form-item) {
  margin-bottom: 18px;
}

:deep(.n-form-item:last-of-type) {
  margin-bottom: 24px;
}

:deep(.n-form-item .n-form-item-feedback-wrapper) {
  min-height: 0;
}

:deep(.n-form-item .n-form-item-label) {
  display: none;
}

.login-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
}

.footer-text {
  font-size: 13px;
  color: var(--lg-text-tertiary);
}

.footer-link {
  font-size: 13px;
  font-weight: 500;
  color: var(--lg-text-secondary);
  text-decoration: none;
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: var(--lg-accent);
}
</style>
