import { clearSession, readSession, saveSession } from './session'
import type {
  EventItem,
  EventPayload,
  TodoItem,
  TodoPayload,
  TokenResponse,
  User,
} from './types'

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) || '/api'
export const SESSION_EXPIRED_EVENT = 'woodashboard:session-expired'

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

let refreshInFlight: Promise<TokenResponse> | null = null

async function responseError(response: Response): Promise<ApiError> {
  let message = `Request failed (${response.status})`
  try {
    const body = (await response.json()) as { message?: string }
    if (body.message) message = body.message
  } catch {
    // Keep the status-based message when the server returned no JSON body.
  }
  return new ApiError(message, response.status)
}

async function refreshTokens(): Promise<TokenResponse> {
  if (refreshInFlight) return refreshInFlight

  refreshInFlight = (async () => {
    const session = readSession()
    if (!session) throw new ApiError('Your session has expired. Please sign in again.', 401)

    const response = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    })

    if (!response.ok) {
      const error = await responseError(response)
      clearSession()
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
      throw error
    }

    const tokens = (await response.json()) as TokenResponse
    saveSession(tokens)
    return tokens
  })()

  try {
    return await refreshInFlight
  } finally {
    refreshInFlight = null
  }
}

async function request<T>(
  path: string,
  init: RequestInit = {},
  authenticated = true,
  retryAfterRefresh = true,
): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const session = readSession()
  if (authenticated && session) {
    headers.set('Authorization', `Bearer ${session.accessToken}`)
  }

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers })

  if (response.status === 401 && authenticated && retryAfterRefresh && path !== '/auth/refresh') {
    await refreshTokens()
    return request<T>(path, init, authenticated, false)
  }

  if (!response.ok) throw await responseError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

function queryString(values: Record<string, string | boolean | undefined>): string {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined) query.set(key, String(value))
  }
  const result = query.toString()
  return result ? `?${result}` : ''
}

async function logout(): Promise<void> {
  const current = readSession()
  if (!current) return

  const sendLogout = (tokens: TokenResponse) =>
    fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokens.accessToken}`,
      },
      body: JSON.stringify({ refreshToken: tokens.refreshToken }),
    })

  try {
    let response = await sendLogout(current)
    if (response.status === 401) {
      const renewedTokens = await refreshTokens()
      response = await sendLogout(renewedTokens)
    }
    if (!response.ok) throw await responseError(response)
  } finally {
    clearSession()
  }
}

export const api = {
  login: (login: string, password: string) =>
    request<TokenResponse>(
      '/auth/login',
      { method: 'POST', body: JSON.stringify({ login, password }) },
      false,
    ),

  register: (login: string, password: string) =>
    request<User>(
      '/auth/register',
      { method: 'POST', body: JSON.stringify({ login, password }) },
      false,
    ),

  getMe: () => request<User>('/me'),

  listEvents: (from?: string, to?: string) =>
    request<EventItem[]>(`/events${queryString({ from, to })}`),

  createEvent: (payload: EventPayload) =>
    request<EventItem>('/events', { method: 'POST', body: JSON.stringify(payload) }),

  updateEvent: (id: string, payload: Partial<EventPayload>) =>
    request<EventItem>(`/events/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),

  deleteEvent: (id: string) =>
    request<void>(`/events/${encodeURIComponent(id)}`, { method: 'DELETE' }),

  listTodos: (userId: string, completed?: boolean) =>
    request<TodoItem[]>(
      `/todos/${encodeURIComponent(userId)}${queryString({ completed })}`,
    ),

  createTodo: (payload: TodoPayload) =>
    request<TodoItem>('/todos', { method: 'POST', body: JSON.stringify(payload) }),

  updateTodo: (id: string, payload: Partial<TodoPayload>) =>
    request<TodoItem>(`/todos/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),

  deleteTodo: (id: string) =>
    request<void>(`/todos/${encodeURIComponent(id)}`, { method: 'DELETE' }),

  adminListUsers: () => request<User[]>('/admin/users'),
  adminDeleteUser: (id: string) =>
    request<void>(`/admin/users/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  adminListEvents: () => request<EventItem[]>('/admin/events'),
  adminListTodos: () => request<TodoItem[]>('/admin/todos'),

  logout,
}
