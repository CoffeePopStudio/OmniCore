<template>
  <n-card title="Register Web Panel Account" style="max-width: 400px; margin: 80px auto">
    <n-alert v-if="!hasBindToken" type="warning" style="margin-bottom: 16px">
      To register, first run <code>/oc web</code> in-game and click the generated link.
    </n-alert>
    <n-form ref="formRef" :model="formData" :rules="rules" @submit.prevent="handleRegister">
      <n-form-item label="UUID" path="uuid">
        <n-input v-model:value="formData.uuid" placeholder="Minecraft UUID" :disabled="bindTokenLoaded" />
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
    <p style="margin-top: 16px; text-align: center">
      Already have an account?
      <router-link to="/login">Login</router-link>
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
const bindTokenLoaded = ref(false)
const hasBindToken = ref(false)

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
  if (bindToken) {
    hasBindToken.value = true
  }
  if (uuidParam) {
    formData.uuid = uuidParam
    bindTokenLoaded.value = true
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
