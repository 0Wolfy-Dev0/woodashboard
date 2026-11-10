import type { Session, TokenResponse } from './types'

const SESSION_KEY = 'woodashboard.session'
export const SESSION_STORAGE_KEY = SESSION_KEY

function accessTokenSubject(accessToken: string): string | null {
  try {
    const payload = accessToken.split('.')[1]
    if (!payload) return null
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    const claims = JSON.parse(atob(padded)) as { sub?: unknown }
    return typeof claims.sub === 'string' ? claims.sub : null
  } catch {
    return null
  }
}

export function readSession(): Session | null {
  try {
    const stored = localStorage.getItem(SESSION_KEY)
    if (!stored) return null

    const session = JSON.parse(stored) as Session
    if (!session.accessToken || !session.refreshToken || !session.expiresAt) {
      localStorage.removeItem(SESSION_KEY)
      return null
    }
    const userId = session.userId || accessTokenSubject(session.accessToken)
    if (!userId) {
      localStorage.removeItem(SESSION_KEY)
      return null
    }
    return { ...session, userId }
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function saveSession(tokens: TokenResponse): Session {
  const session: Session = {
    ...tokens,
    expiresAt: Date.now() + tokens.expiresIn * 1000,
    userId: accessTokenSubject(tokens.accessToken) ?? '',
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function clearSession(): void {
  localStorage.removeItem(SESSION_KEY)
}
