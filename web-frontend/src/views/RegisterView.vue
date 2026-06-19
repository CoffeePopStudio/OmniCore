<template>
  <div class="register-page">
    <div class="glass-card register-card">
      <h1 class="text-large-title register-title">Register</h1>
      <p class="text-callout register-subtitle">Create your Web Panel account</p>
      <n-form ref="formRef" :model="formData" :rules="rules" @submit.prevent="handleRegister">
        <n-form-item label="UUID" path="uuid">
          <n-input v-model:value="formData.uuid" placeholder="Minecraft UUID" />
        </n-form-item>
        <n-form-item label="Username" path="username">
          <n-input v-model:value="formData.username" placeholder="Choose a username" />
        </n-form-item>
        <n-form-item label="Password" path="password">
          <n-input v-model:value="formData.password" type="password" show-password-on="click" placeholder="Choose a password" />
        </n-form-item>
        <n-form-item label="Confirm Password" path="confirmPassword">
          <n-input v-model:value="formData.confirmPassword" type="password" show-password-on="click" placeholder="Confirm your password" />
        </n-form-item>
        <n-button type="primary" attr-type="submit" :loading="loading" block>
          Register
        </n-button>
      </n-form>
      <p class="footer-link">
        Already have an account?
        <router-link to="/login">Login</router-link>
      </p>
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

const formData = reactive({
  uuid: '',
  username: '',
  password: '',
  confirmPassword: '',
})

const rules = {
  uuid: [{ required: true, message: 'UUID is required' }],
  username: [{ required: true, message: 'Username is required' }],
  password: [
    { required: true, message: 'Password is required' },
    { min: 6, message: 'Password must be at least 6 characters' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm your password' },
    {
      validator: (_: any, value: string) => value === formData.password,
      message: 'Passwords do not match',
    },
  ],
}

onMounted(() => {
  const bindToken = route.query.bind_token as string
  const uuidParam = route.query.uuid as string
  if (uuidParam) {
    formData.uuid = uuidParam
  }
})

async function handleRegister() {
  try {
    loading.value = true
    const bindToken = route.query.bind_token as string
    if (bindToken) {
      await auth.bind(bindToken, formData.username, formData.password)
    } else {
      await auth.register(formData.uuid, formData.username, formData.password)
    }
    message.success('Registration successful')
    router.push('/dashboard')
  } catch (e: any) {
    message.error(e.message || 'Registration failed')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.register-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  padding: 36px 32px 32px;
}

.register-title {
  margin-bottom: 4px;
  text-align: center;
}

.register-subtitle {
  text-align: center;
  color: var(--lg-text-secondary);
  margin-bottom: 28px;
}

.footer-link {
  margin-top: 24px;
  text-align: center;
  color: var(--lg-text-tertiary);
  font-size: 14px;
}

.footer-link a {
  color: var(--lg-text-secondary);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.footer-link a:hover {
  color: var(--lg-accent);
}
</style>
