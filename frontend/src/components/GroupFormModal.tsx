import { useState, type FormEvent } from 'react'
import { LoaderCircle, Save, Users } from 'lucide-react'
import { Modal } from './Modal'
import type { Group, GroupPayload } from '../types'

interface GroupFormModalProps {
  group?: Group
  onClose: () => void
  onSave: (payload: GroupPayload) => Promise<void>
}

export function GroupFormModal({ group, onClose, onSave }: GroupFormModalProps) {
  const [name, setName] = useState(group?.name ?? '')
  const [description, setDescription] = useState(group?.description ?? '')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      await onSave({ name: name.trim(), description: description.trim() || null })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not save this group.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={group ? 'Edit group' : 'Create a group'}
      eyebrow={group ? 'Group settings' : 'Share with your people'}
      onClose={onClose}
      footer={
        <>
          <span className="modal-footer-spacer" />
          <button className="button button-secondary" type="button" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button className="button button-primary" type="submit" form="group-form" disabled={saving}>
            {saving ? <LoaderCircle className="spin" size={16} /> : <Save size={16} />}
            {group ? 'Save changes' : 'Create group'}
          </button>
        </>
      }
    >
      <form id="group-form" className="form-stack" onSubmit={submit}>
        <div className="group-form-note">
          <span className="group-form-note-icon"><Users size={17} /></span>
          <p>Group members can see and edit events and tasks shared with the group.</p>
        </div>
        <label className="field">
          <span>Group name</span>
          <input autoFocus required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Family, Home lab, Friends" />
        </label>
        <label className="field">
          <span>Description <small>Optional</small></span>
          <textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="What is this group for?" />
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
