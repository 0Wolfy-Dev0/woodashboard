import type { Session, TokenResponse } from './types'

const SESSION_KEY = 'woodashboard.session'

export function readSession(): Session | null {
  try {
    const stored = localStorage.getItem(SESSION_KEY)
    if (!stored) return null

    const session = JSON.parse(stored) as Session
    if (!session.accessToken || !session.refreshToken || !session.expiresAt) {
      localStorage.removeItem(SESSION_KEY)
      return null
    }
    return session
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function saveSession(tokens: TokenResponse): Session {
  const session: Session = {
    ...tokens,
    expiresAt: Date.now() + tokens.expiresIn * 1000,
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function clearSession(): void {
  localStorage.removeItem(SESSION_KEY)
}
