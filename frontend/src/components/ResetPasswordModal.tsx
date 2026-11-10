import { useState, type FormEvent } from 'react'
import { KeyRound, LoaderCircle, ShieldCheck } from 'lucide-react'
import { Modal } from './Modal'
import type { User } from '../types'

interface ResetPasswordModalProps {
  user: User
  onClose: () => void
  onSave: (newPassword: string) => Promise<void>
}

export function ResetPasswordModal({ user, onClose, onSave }: ResetPasswordModalProps) {
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (newPassword.length < 8) {
      setError('The new password must be at least 8 characters long.')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('The new passwords do not match.')
      return
    }
    setSaving(true)
    try {
      await onSave(newPassword)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not reset this password.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={`Reset ${user.login}'s password`}
      eyebrow="Administrator action"
      onClose={onClose}
      footer={
        <>
          <span className="modal-footer-spacer" />
          <button className="button button-secondary" type="button" onClick={onClose} disabled={saving}>Cancel</button>
          <button className="button button-primary" type="submit" form="reset-password-form" disabled={saving}>
            {saving ? <LoaderCircle className="spin" size={16} /> : <KeyRound size={16} />}
            Reset password
          </button>
        </>
      }
    >
      <form id="reset-password-form" className="form-stack" onSubmit={submit}>
        <div className="group-form-note">
          <span className="group-form-note-icon"><ShieldCheck size={17} /></span>
          <p>Every active session for {user.login} will be signed out after the reset.</p>
        </div>
        <label className="field">
          <span>New password</span>
          <input
            autoFocus
            type="password"
            required
            minLength={8}
            maxLength={128}
            autoComplete="new-password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            placeholder="At least 8 characters"
          />
        </label>
        <label className="field">
          <span>Confirm new password</span>
          <input
            type="password"
            required
            minLength={8}
            maxLength={128}
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            placeholder="Repeat the new password"
          />
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
