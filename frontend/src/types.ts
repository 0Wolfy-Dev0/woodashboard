export type Role = 'USER' | 'ADMIN'

export interface User {
  id: string
  login: string
  role: Role
  creationTime: string
  lastModificationTime: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface Session extends TokenResponse {
  expiresAt: number
}

export interface EventItem {
  id: string
  name: string
  description: string | null
  userId: string
  startTime: string
  endTime: string
  creationTime: string
  lastModificationTime: string
}

export interface EventPayload {
  name: string
  description: string | null
  startTime: string
  endTime: string
}

export interface TodoItem {
  id: string
  name: string
  description: string | null
  userId: string
  completed: boolean
  creationTime: string
  lastModificationTime: string
}

export interface TodoPayload {
  name: string
  description: string | null
  completed?: boolean
}

export type Page = 'home' | 'calendar' | 'todos' | 'profile' | 'admin'

export type ToastNotice = {
  id: number
  message: string
  kind: 'success' | 'error' | 'info'
}
