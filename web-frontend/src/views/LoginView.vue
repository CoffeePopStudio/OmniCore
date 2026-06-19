<template>
  <n-card title="Login" style="max-width: 400px; margin: 80px auto">
    <n-alert v-if="hasBindToken" type="info" style="margin-bottom: 16px">
      A bind token was detected. If you haven't registered yet, please go to
      <router-link to="/register">Register</router-link> first.
    </n-alert>
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
