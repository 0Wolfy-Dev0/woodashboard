import { useState, type FormEvent } from 'react'
import { ArrowRight, CalendarDays, Eye, EyeOff, LockKeyhole, Sparkles, UserRound } from 'lucide-react'
import { api } from '../api'
import type { TokenResponse } from '../types'

interface LoginPageProps {
  onAuthenticated: (tokens: TokenResponse) => void
}

export function LoginPage({ onAuthenticated }: LoginPageProps) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [login, setLogin] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)

    try {
      if (mode === 'register') {
        await api.register(login.trim(), password)
        setSuccess('Your account is ready. Signing you in…')
      }
      const tokens = await api.login(login.trim(), password)
      onAuthenticated(tokens)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not sign in. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-screen">
      <section className="auth-story">
        <div className="auth-brand">
          <span className="brand-mark"><CalendarDays size={21} strokeWidth={2.2} /></span>
          <span>woodashboard</span>
        </div>
        <div className="auth-story-copy">
          <span className="eyebrow eyebrow-light"><Sparkles size={14} /> A little more room to think</span>
          <h1>Your home life,<br />in good rhythm.</h1>
          <p>Keep plans, projects, and little reminders together in one calm place.</p>
        </div>
        <div className="auth-story-card">
          <div className="story-card-top"><span className="story-dot" /><span>YOUR WEEK, AT A GLANCE</span><span className="story-card-date">MON — SUN</span></div>
          <div className="story-week">
            {[
              ['M', '12', true], ['T', '13', false], ['W', '14', false], ['T', '15', true], ['F', '16', false], ['S', '17', false], ['S', '18', true],
            ].map(([day, number, active], index) => (
              <div className={`story-day${active ? ' story-day-active' : ''}`} key={`${day}-${index}`}>
                <span>{day}</span><b>{number}</b><i className={active ? 'story-dot-active' : ''} />
              </div>
            ))}
          </div>
          <div className="story-card-note"><span className="story-note-icon"><Sparkles size={15} /></span><span>Make space for what matters.</span><span className="story-note-line" /></div>
        </div>
        <div className="auth-story-footer"><span>Made for your home, your way.</span><span>01 / 03</span></div>
      </section>

      <section className="auth-form-side">
        <div className="auth-form-card">
          <span className="eyebrow">WELCOME TO YOUR DASHBOARD</span>
          <h2>{mode === 'login' ? 'Good to have you back.' : 'Let’s get you set up.'}</h2>
          <p className="auth-subtitle">{mode === 'login' ? 'Sign in to pick up where you left off.' : 'Create an account to start planning your days.'}</p>

          <div className="auth-tabs" role="tablist" aria-label="Account access">
            <button type="button" role="tab" aria-selected={mode === 'login'} className={mode === 'login' ? 'auth-tab active' : 'auth-tab'} onClick={() => { setMode('login'); setError(''); setSuccess('') }}>Sign in</button>
            <button type="button" role="tab" aria-selected={mode === 'register'} className={mode === 'register' ? 'auth-tab active' : 'auth-tab'} onClick={() => { setMode('register'); setError(''); setSuccess('') }}>Create account</button>
          </div>

          <form className="form-stack auth-form" onSubmit={submit}>
            <label className="field">
              <span>Login</span>
              <div className="input-with-icon"><UserRound size={17} /><input autoComplete="username" autoFocus required minLength={mode === 'register' ? 3 : undefined} maxLength={64} value={login} onChange={(e) => setLogin(e.target.value)} placeholder="Your username" /></div>
            </label>
            <label className="field">
              <span>Password</span>
              <div className="input-with-icon"><LockKeyhole size={17} /><input autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required minLength={mode === 'register' ? 8 : undefined} maxLength={128} type={showPassword ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="At least 8 characters" /><button className="password-toggle" type="button" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <EyeOff size={16} /> : <Eye size={16} />}</button></div>
              {mode === 'register' && <small className="field-hint">Use at least 8 characters.</small>}
            </label>
            {error && <p className="form-error" role="alert">{error}</p>}
            {success && <p className="form-success" role="status">{success}</p>}
            <button className="button button-primary auth-submit" type="submit" disabled={loading}>
              {loading ? 'One moment…' : mode === 'login' ? 'Sign in' : 'Create account'}
              {!loading && <ArrowRight size={17} />}
            </button>
          </form>
          <div className="auth-safe-note"><span className="safe-lock"><LockKeyhole size={13} /></span><span>Your dashboard is just for you.</span></div>
        </div>
        <footer className="auth-copyright">WOODASH HOME DASHBOARD <span>·</span> A calmer way to keep it together</footer>
      </section>
    </main>
  )
}
