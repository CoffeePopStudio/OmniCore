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
  page?: number
  pageSize?: number
  player?: string
  world?: string
  time_from?: string
  time_to?: string
  block_type?: string
  item_type?: string
}

export interface BlockRecord {
  timestamp: string
  player_name: string
  action: string
  world: string
  x: number
  y: number
  z: number
  old_block_type: string
  new_block_type: string
}

export interface ContainerRecord {
  timestamp: string
  player_name: string
  action: string
  world: string
  x: number
  y: number
  z: number
  item_type: string
  item_amount: number
}

export interface InventoryRecord {
  timestamp: string
  player_name: string
  action: string
  item_type: string
  item_amount: number
}

export type QueryRecord = BlockRecord | ContainerRecord | InventoryRecord

export type QueryType = 'blocks' | 'containers' | 'inventory'

export interface QueryResponse {
  records: QueryRecord[]
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

export interface RollbackProgressResponse {
  progress: number
}

export interface LogsResponse {
  content: string
  total_lines: number
  lines_requested: number
}

export interface StatsResponse {
  count: number
}

export interface RollbackFilters {
  player: string
  world: string
  blockType: string
  radius: number
  center: string
}
