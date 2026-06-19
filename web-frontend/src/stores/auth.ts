import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/api/client'
import type { AuthResponse } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const uuid = ref(localStorage.getItem('uuid') || '')
  const username = ref(localStorage.getItem('username') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(data: AuthResponse) {
    token.value = data.token
    uuid.value = data.uuid
    username.value = data.username
    localStorage.setItem('token', data.token)
    localStorage.setItem('uuid', data.uuid)
    localStorage.setItem('username', data.username)
  }

  async function login(uuidVal: string, password: string) {
    const data = await api.login(uuidVal, password)
    setAuth(data)
  }

  async function register(uuidVal: string, usernameVal: string, password: string) {
    const data = await api.register(uuidVal, usernameVal, password)
    setAuth(data)
  }

  async function bind(bindToken: string, usernameVal: string, password: string) {
    const data = await api.bind(bindToken, usernameVal, password)
    setAuth(data)
  }

  async function autoLogin(bindToken: string) {
    const data = await api.autoLogin(bindToken)
    if (data.status === 'ok' && data.token) {
      setAuth({ token: data.token, uuid: data.uuid || '', username: '' })
      return true
    }
    return false
  }

  function logout() {
    token.value = ''
    uuid.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('uuid')
    localStorage.removeItem('username')
  }

  function getQueryParam(param: string): string | null {
    const params = new URLSearchParams(window.location.search)
    if (params.has(param)) return params.get(param)
    const hash = window.location.hash
    const hashIndex = hash.indexOf('?')
    if (hashIndex !== -1) {
      const hashParams = new URLSearchParams(hash.slice(hashIndex))
      if (hashParams.has(param)) return hashParams.get(param)
    }
    return null
  }

  async function checkAutoLogin() {
    const bindToken = getQueryParam('bind_token')
    if (bindToken) {
      const success = await autoLogin(bindToken)
      if (success) {
        const cleanUrl = window.location.origin + window.location.pathname + window.location.hash.split('?')[0]
        window.history.replaceState({}, '', cleanUrl)
        return true
      }
    }
    return false
  }

  return { token, uuid, username, isLoggedIn, login, register, bind, autoLogin, logout, checkAutoLogin, setAuth }
})
