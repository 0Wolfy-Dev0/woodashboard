import { useState, type FormEvent } from 'react'
import { BadgeCheck, CalendarDays, Copy, KeyRound, LoaderCircle, LockKeyhole, LogOut, ShieldCheck, UserRound } from 'lucide-react'
import { api } from '../api'
import { formatLongDate } from '../format'
import type { Session, ToastNotice, User } from '../types'

interface ProfilePageProps {
  user: User
  session: Session | null
  onSignOut: () => Promise<void>
  onNotice: (message: string, kind?: ToastNotice['kind']) => void
}

export function ProfilePage({ user, session, onSignOut, onNotice }: ProfilePageProps) {
  const [signingOut, setSigningOut] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [changingPassword, setChangingPassword] = useState(false)

  async function changePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPasswordError('')
    if (newPassword.length < 8) {
      setPasswordError('The new password must be at least 8 characters long.')
      return
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('The new passwords do not match.')
      return
    }
    setChangingPassword(true)
    try {
      await api.changePassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      onNotice('Password updated.')
    } catch (error) {
      setPasswordError(error instanceof Error ? error.message : 'Could not update your password.')
    } finally {
      setChangingPassword(false)
    }
  }

  async function copyId() {
    try {
      await navigator.clipboard.writeText(user.id)
      onNotice('Account ID copied.')
    } catch {
      onNotice('Could not copy the account ID.', 'error')
    }
  }

  async function signOut() {
    setSigningOut(true)
    try {
      await onSignOut()
    } catch (error) {
      onNotice(error instanceof Error ? error.message : 'You were signed out locally.', 'error')
    } finally {
      setSigningOut(false)
    }
  }

  return (
    <div className="page-content profile-page">
      <div className="page-heading">
        <div><span className="eyebrow">YOUR ACCOUNT</span><h1>Profile</h1><p>A few details about your dashboard account.</p></div>
      </div>

      <div className="profile-layout">
        <section className="surface-card profile-identity-card">
          <div className="profile-avatar"><UserRound size={29} /></div>
          <span className="eyebrow">WOODASH MEMBER</span>
          <h2>{user.login}</h2>
          <span className={`role-badge ${user.role === 'ADMIN' ? 'role-admin' : 'role-user'}`}>
            {user.role === 'ADMIN' ? <ShieldCheck size={14} /> : <BadgeCheck size={14} />}
            {user.role === 'ADMIN' ? 'Administrator' : 'Member'}
          </span>
          <p className="profile-note">Your home dashboard is ready whenever you are.</p>
          <div className="profile-id-row"><span>ACCOUNT ID</span><button onClick={copyId} title="Copy account ID"><code>{user.id.slice(0, 8)}…{user.id.slice(-4)}</code><Copy size={14} /></button></div>
        </section>

        <div className="profile-details-column">
          <section className="surface-card profile-details-card">
            <div className="section-heading"><div><span className="eyebrow">ACCOUNT DETAILS</span><h2>Your information</h2></div><span className="aside-heading-icon"><UserRound size={17} /></span></div>
            <div className="profile-detail-row"><span>Login</span><strong>{user.login}</strong></div>
            <div className="profile-detail-row"><span>Account type</span><strong>{user.role === 'ADMIN' ? 'Administrator' : 'Standard user'}</strong></div>
            <div className="profile-detail-row"><span>Joined</span><strong>{formatLongDate(user.creationTime)}</strong></div>
            <div className="profile-detail-row"><span>Last updated</span><strong>{formatLongDate(user.lastModificationTime)}</strong></div>
          </section>

          <section className="surface-card profile-password-card">
            <div className="section-heading"><div><span className="eyebrow">SECURITY</span><h2>Change password</h2></div><span className="aside-heading-icon"><LockKeyhole size={17} /></span></div>
            <form className="profile-password-form" onSubmit={changePassword}>
              <label className="field">
                <span>Current password</span>
                <input type="password" required autoComplete="current-password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} placeholder="Your current password" />
              </label>
              <div className="form-grid-two">
                <label className="field">
                  <span>New password</span>
                  <input type="password" required minLength={8} maxLength={128} autoComplete="new-password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="At least 8 characters" />
                </label>
                <label className="field">
                  <span>Confirm new password</span>
                  <input type="password" required minLength={8} maxLength={128} autoComplete="new-password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} placeholder="Repeat the new password" />
                </label>
              </div>
              {passwordError && <p className="form-error" role="alert">{passwordError}</p>}
              <button className="button button-primary button-small profile-password-submit" type="submit" disabled={changingPassword}>
                {changingPassword ? <LoaderCircle className="spin" size={14} /> : <KeyRound size={14} />}
                {changingPassword ? 'Updating…' : 'Update password'}
              </button>
            </form>
          </section>

          <section className="surface-card profile-session-card">
            <div className="section-heading"><div><span className="eyebrow">SECURITY</span><h2>Your session</h2></div><span className="aside-heading-icon"><KeyRound size={17} /></span></div>
            <div className="session-status"><span className="session-status-dot" /><span><strong>Signed in</strong><small>Access tokens renew automatically when needed.</small></span></div>
            {session && <p className="session-expiry">Current access token expires around {new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(new Date(session.expiresAt))}.</p>}
            <button className="button button-danger-quiet profile-signout" onClick={() => void signOut()} disabled={signingOut}><LogOut size={16} />{signingOut ? 'Signing out…' : 'Sign out of this device'}</button>
          </section>

          <div className="profile-footnote"><CalendarDays size={15} /> Your login and join date are read-only; you can change your password above.</div>
        </div>
      </div>
    </div>
  )
}
