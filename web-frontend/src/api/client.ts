const BASE_URL = window.location.origin

function getTokenParam(): string {
  const token = localStorage.getItem('token')
  return token ? `token=${encodeURIComponent(token)}` : ''
}

function buildFormData(data: Record<string, any>): URLSearchParams {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(data)) {
    if (value !== undefined && value !== null && value !== '') {
      params.append(key, String(value))
    }
  }
  return params
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const token = getTokenParam()
  const separator = url.includes('?') ? '&' : '?'
  const fullUrl = token ? `${BASE_URL}${url}${separator}${token}` : `${BASE_URL}${url}`

  const response = await fetch(fullUrl, {
    ...options,
    headers: {
      ...options.headers,
    },
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.error || `HTTP ${response.status}: ${response.statusText}`)
  }

  return response.json()
}

export interface HealthResponse {
  status: string
  version?: string
}

export interface AuthResponse {
  token: string
  uuid: string
  username: string
}

export interface AutoLoginResponse {
  status: string
  token?: string
  uuid?: string
  bind_token?: string
}

export interface QueryParams {
  player?: string
  type?: string
  world?: string
  x?: number
  y?: number
  z?: number
  radius?: number
  timeStart?: string
  timeEnd?: string
  page?: number
  pageSize?: number
}

export interface QueryResponse {
  records: any[]
  page: number
  page_size: number
}

export interface RollbackPreviewResponse {
  preview: Record<string, string>
  count: number
}

export interface RollbackExecuteResponse {
  status: string
  ticket?: string
  error?: string
}

export interface StatsResponse {
  count: number
}

export const api = {
  health(): Promise<HealthResponse> {
    return request<HealthResponse>('/api/health')
  },

  async login(uuid: string, password: string): Promise<AuthResponse> {
    const formData = buildFormData({ uuid, password })
    return request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: formData,
    })
  },

  async register(uuid: string, username: string, password: string): Promise<AuthResponse> {
    const formData = buildFormData({ uuid, username, password })
    return request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: formData,
    })
  },

  async bind(bindToken: string, username: string, password: string): Promise<AuthResponse> {
    const formData = buildFormData({ bind_token: bindToken, username, password })
    return request<AuthResponse>('/api/auth/bind', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: formData,
    })
  },

  autoLogin(bindToken: string): Promise<AutoLoginResponse> {
    return request<AutoLoginResponse>(`/api/auth/auto-login?bind_token=${encodeURIComponent(bindToken)}`)
  },

  async refreshToken(token: string): Promise<{ token: string }> {
    const formData = buildFormData({ token })
    return request<{ token: string }>('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: formData,
    })
  },

  queryBlocks(params: QueryParams): Promise<QueryResponse> {
    return request<QueryResponse>(`/api/query/blocks${buildQueryString(params)}`)
  },

  queryContainers(params: QueryParams): Promise<QueryResponse> {
    return request<QueryResponse>(`/api/query/containers${buildQueryString(params)}`)
  },

  queryInventory(params: QueryParams): Promise<QueryResponse> {
    return request<QueryResponse>(`/api/query/inventory${buildQueryString(params)}`)
  },

  rollbackPreview(params: Record<string, any>): Promise<RollbackPreviewResponse> {
    return request<RollbackPreviewResponse>(`/api/rollback/preview${buildQueryString(params)}`)
  },

  async rollbackExecute(params: Record<string, any>): Promise<RollbackExecuteResponse> {
    const formData = buildFormData(params)
    return request<RollbackExecuteResponse>('/api/rollback/execute', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: formData,
    })
  },

  statsBlocks(): Promise<StatsResponse> {
    return request<StatsResponse>('/api/stats/blocks')
  },

  statsContainers(): Promise<StatsResponse> {
    return request<StatsResponse>('/api/stats/containers')
  },

  statsInventory(): Promise<StatsResponse> {
    return request<StatsResponse>('/api/stats/inventory')
  },
}

function buildQueryString(params: Record<string, any>): string {
  const parts: string[] = []
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    }
  }
  return parts.length > 0 ? `?${parts.join('&')}` : ''
}
