<template>
  <n-card title="Login" style="max-width: 400px; margin: 80px auto">
    <n-form ref="formRef" :model="formData" :rules="rules" @submit.prevent="handleLogin">
      <n-form-item label="UUID" path="uuid">
        <n-input v-model:value="formData.uuid" placeholder="Enter your Minecraft UUID" />
      </n-form-item>
      <n-form-item label="Password" path="password">
        <n-input v-model:value="formData.password" type="password" show-password-on="click" placeholder="Enter password" />
      </n-form-item>
      <n-button type="primary" attr-type="submit" :loading="loading" block>
        Login
      </n-button>
    </n-form>
    <p style="margin-top: 16px; text-align: center">
      Don't have an account?
      <router-link to="/register">Register</router-link>
    </p>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMessage } from 'naive-ui'

const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const loading = ref(false)

const formData = reactive({
  uuid: '',
  password: '',
})

const rules = {
  uuid: [{ required: true, message: 'Please enter your UUID' }],
  password: [{ required: true, message: 'Please enter your password' }],
}

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
