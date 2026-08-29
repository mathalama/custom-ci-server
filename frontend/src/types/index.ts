export * from './project'
export * from './build'
export * from './runner'
export * from './websocket'

export interface ApiResponse<T> {
  success: boolean
  data: T
  error: string | null
  timestamp: string
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ToastMessage {
  id: string
  type: 'success' | 'error' | 'info' | 'warning'
  title: string
  message?: string
  duration?: number
}
