import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export function bind(uuid: string) {
  return client.post('/bind', { uuid })
}

export function register(uuid: string, username: string, password: string, playerName: string) {
  return client.post('/register', { uuid, username, password, playerName })
}

export function login(username: string, password: string) {
  return client.post('/login', { username, password })
}

export function refresh(token: string) {
  return client.post('/refresh', { token })
}

export function queryBlocks(params: Record<string, any>) {
  return client.get('/query/blocks', { params })
}

export function queryContainers(params: Record<string, any>) {
  return client.get('/query/containers', { params })
}

export function queryInventory(params: Record<string, any>) {
  return client.get('/query/inventory', { params })
}

export function rollbackPreview(body: Record<string, any>) {
  return client.post('/rollback/preview', body)
}

export function rollbackExecute(body: Record<string, any>) {
  return client.post('/rollback/execute', body)
}

export function rollbackProgress(ticket: string) {
  return client.get('/rollback/progress', { params: { ticket } })
}

export default client
