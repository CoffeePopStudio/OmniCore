const BASE_URL = window.location.origin

function getTokenParam(): string {
  const token = localStorage.getItem('token')
  return token ? `token=${encodeURIComponent(token)}` : ''
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const token = getTokenParam()
  const separator = url.includes('?') ? '&' : '?'
  const fullUrl = token ? `${BASE_URL}${url}${separator}${token}` : `${BASE_URL}${url}`

  const response = await fetch(fullUrl, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`)
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
  total: number
  page: number
  pageSize: number
}

export interface RollbackPreviewParams {
  timeAmount: number
  timeUnit: string
  player?: string
  world?: string
  blockType?: string
  radius?: number
  x?: number
  y?: number
  z?: number
}

export interface RollbackExecuteParams {
  timeAmount: number
  timeUnit: string
  player?: string
  world?: string
  blockType?: string
  radius?: number
  x?: number
  y?: number
  z?: number
  confirm: boolean
}

export interface RollbackPreviewResponse {
  affectedLocations: number
  summary: string
  records: any[]
}

export interface RollbackExecuteResponse {
  success: boolean
  taskId?: string
  message: string
  affectedLocations?: number
}

export const api = {
  health(): Promise<HealthResponse> {
    return request<HealthResponse>('/api/health')
  },

  login(uuid: string, password: string): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ uuid, password }),
    })
  },

  register(uuid: string, username: string, password: string): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ uuid, username, password }),
    })
  },

  bind(bindToken: string, username: string, password: string): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/bind', {
      method: 'POST',
      body: JSON.stringify({ bindToken, username, password }),
    })
  },

  autoLogin(bindToken: string): Promise<AuthResponse> {
    return request<AuthResponse>(`/api/auth/auto-login?bindToken=${encodeURIComponent(bindToken)}`)
  },

  refreshToken(token: string): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ token }),
    })
  },

  queryBlocks(params: QueryParams): Promise<QueryResponse> {
    const query = buildQueryString(params)
    return request<QueryResponse>(`/api/query/blocks${query}`)
  },

  queryContainers(params: QueryParams): Promise<QueryResponse> {
    const query = buildQueryString(params)
    return request<QueryResponse>(`/api/query/containers${query}`)
  },

  queryInventory(params: QueryParams): Promise<QueryResponse> {
    const query = buildQueryString(params)
    return request<QueryResponse>(`/api/query/inventory${query}`)
  },

  rollbackPreview(params: RollbackPreviewParams): Promise<RollbackPreviewResponse> {
    const query = buildQueryString(params as any)
    return request<RollbackPreviewResponse>(`/api/rollback/preview${query}`)
  },

  rollbackExecute(params: RollbackExecuteParams): Promise<RollbackExecuteResponse> {
    return request<RollbackExecuteResponse>('/api/rollback/execute', {
      method: 'POST',
      body: JSON.stringify(params),
    })
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
