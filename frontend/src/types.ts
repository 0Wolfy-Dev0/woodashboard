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

export interface Group {
  id: string
  name: string
  description: string | null
  ownerId: string
  ownerLogin: string
  members: User[]
  creationTime: string
  lastModificationTime: string
}

export interface GroupPayload {
  name: string
  description: string | null
}

export type RecurrenceUnit = 'DAYS' | 'WEEKS' | 'MONTHS' | 'YEARS'

export interface TodoFolder {
  id: string
  name: string
  userId: string
  creationTime: string
  lastModificationTime: string
}

export interface TodoFolderPayload {
  name: string
}

export interface EventItem {
  id: string
  name: string
  description: string | null
  userId: string
  groupId: string | null
  allDay: boolean
  startTime: string
  endTime: string
  seriesStartTime: string
  seriesEndTime: string
  recurrenceUnit: RecurrenceUnit | null
  recurrenceInterval: number | null
  recurrenceEnd: string | null
  creationTime: string
  lastModificationTime: string
}

export interface EventPayload {
  name: string
  description: string | null
  startTime: string
  endTime: string
  allDay: boolean
  recurrenceUnit: RecurrenceUnit | null
  recurrenceInterval: number | null
  recurrenceEnd: string | null
  groupId: string | null
}

export interface TodoItem {
  id: string
  name: string
  description: string | null
  userId: string
  groupId: string | null
  folderId: string | null
  completed: boolean
  creationTime: string
  lastModificationTime: string
}

export interface TodoPayload {
  name: string
  description: string | null
  completed?: boolean
  groupId: string | null
  folderId?: string | null
}

export type Page = 'home' | 'calendar' | 'todos' | 'groups' | 'profile' | 'admin'

export type ToastNotice = {
  id: number
  message: string
  kind: 'success' | 'error' | 'info'
}
