import { useState, type FormEvent } from 'react'
import { CalendarPlus, LoaderCircle, Trash2 } from 'lucide-react'
import { Modal } from './Modal'
import { fromDateTimeInput, toDateTimeInput } from '../format'
import type { EventItem, EventPayload, Group } from '../types'

interface EventFormModalProps {
  event?: EventItem
  initialStart?: Date
  groups: Group[]
  onClose: () => void
  onSave: (payload: EventPayload) => Promise<void>
  onDelete?: () => Promise<void>
}

function defaultTimes(initialStart?: Date): { start: string; end: string } {
  const start = initialStart ? new Date(initialStart) : new Date()
  if (!initialStart) {
    start.setMinutes(Math.ceil(start.getMinutes() / 15) * 15, 0, 0)
  } else if (start.getHours() === 0 && start.getMinutes() === 0) {
    start.setHours(9, 0, 0, 0)
  }
  const end = new Date(start)
  end.setHours(end.getHours() + 1)
  const toInput = (date: Date) => {
    const pad = (value: number) => String(value).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
  return { start: toInput(start), end: toInput(end) }
}

export function EventFormModal({ event, initialStart, groups, onClose, onSave, onDelete }: EventFormModalProps) {
  const defaults = defaultTimes(initialStart)
  const [name, setName] = useState(event?.name ?? '')
  const [description, setDescription] = useState(event?.description ?? '')
  const [startTime, setStartTime] = useState(event ? toDateTimeInput(event.startTime) : defaults.start)
  const [endTime, setEndTime] = useState(event ? toDateTimeInput(event.endTime) : defaults.end)
  const [groupId, setGroupId] = useState(event?.groupId ?? '')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (new Date(endTime).getTime() < new Date(startTime).getTime()) {
      setError('The end time must be after the start time.')
      return
    }

    setSaving(true)
    try {
      await onSave({
        name: name.trim(),
        description: description.trim() || null,
        startTime: fromDateTimeInput(startTime),
        endTime: fromDateTimeInput(endTime),
        groupId: groupId || null,
      })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not save this event.')
    } finally {
      setSaving(false)
    }
  }

  async function removeEvent() {
    if (!onDelete) return
    setDeleting(true)
    setError('')
    try {
      await onDelete()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not delete this event.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <Modal
      title={event ? 'Edit event' : 'Plan something'}
      eyebrow={event ? 'Your calendar' : 'New calendar event'}
      onClose={onClose}
      footer={
        <>
          {event && onDelete && (
            <button className="button button-danger-quiet" type="button" onClick={removeEvent} disabled={deleting || saving}>
              {deleting ? <LoaderCircle className="spin" size={16} /> : <Trash2 size={16} />}
              Delete
            </button>
          )}
          <span className="modal-footer-spacer" />
          <button className="button button-secondary" type="button" onClick={onClose} disabled={saving || deleting}>
            Cancel
          </button>
          <button className="button button-primary" type="submit" form="event-form" disabled={saving || deleting}>
            {saving ? <LoaderCircle className="spin" size={16} /> : <CalendarPlus size={16} />}
            {event ? 'Save changes' : 'Add event'}
          </button>
        </>
      }
    >
      <form id="event-form" className="form-stack" onSubmit={submit}>
        <label className="field">
          <span>Event name</span>
          <input autoFocus required maxLength={255} value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Check the backups" />
        </label>
        <label className="field">
          <span>Description <small>Optional</small></span>
          <textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Add a little more detail" />
        </label>
        <div className="form-grid-two">
          <label className="field">
            <span>Starts</span>
            <input type="datetime-local" required value={startTime} onChange={(e) => setStartTime(e.target.value)} />
          </label>
          <label className="field">
            <span>Ends</span>
            <input type="datetime-local" required value={endTime} onChange={(e) => setEndTime(e.target.value)} />
          </label>
        </div>
        {event ? (
          <p className="field-hint">
            {event.groupId
              ? `Shared with ${groups.find((group) => group.id === event.groupId)?.name ?? 'a group'}`
              : 'Private event'}
          </p>
        ) : (
          <label className="field">
            <span>Shared with <small>Optional</small></span>
            <select value={groupId} onChange={(e) => setGroupId(e.target.value)}>
              <option value="">Only me</option>
              {groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
            </select>
          </label>
        )}
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
