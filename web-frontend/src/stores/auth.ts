import { defineStore } from 'pinia'
import { api } from '../api/client'

interface AuthState {
  token: string
  uuid: string
  username: string
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('token') || '',
    uuid: localStorage.getItem('uuid') || '',
    username: localStorage.getItem('username') || '',
  }),

  getters: {
    isAuthenticated(): boolean {
      return !!this.token
    },
  },

  actions: {
    async login(uuid: string, password: string) {
      const response = await api.login(uuid, password)
      this.token = response.token
      this.uuid = response.uuid
      this.username = response.username
      this.persist()
    },

    async register(uuid: string, username: string, password: string) {
      const response = await api.register(uuid, username, password)
      this.token = response.token
      this.uuid = response.uuid
      this.username = response.username
      this.persist()
    },

    async bind(bindToken: string, username: string, password: string) {
      const response = await api.bind(bindToken, username, password)
      this.token = response.token
      this.uuid = response.uuid
      this.username = response.username
      this.persist()
    },

    async autoLogin(bindToken: string) {
      const response = await api.autoLogin(bindToken)
      this.token = response.token
      this.uuid = response.uuid
      this.username = response.username
      this.persist()
    },

    async refreshToken() {
      if (!this.token) return
      const response = await api.refreshToken(this.token)
      this.token = response.token
      this.persist()
    },

    logout() {
      this.token = ''
      this.uuid = ''
      this.username = ''
      localStorage.removeItem('token')
      localStorage.removeItem('uuid')
      localStorage.removeItem('username')
    },

    persist() {
      localStorage.setItem('token', this.token)
      localStorage.setItem('uuid', this.uuid)
      localStorage.setItem('username', this.username)
    },
  },
})
