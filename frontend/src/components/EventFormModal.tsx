import { useState, type FormEvent } from 'react'
import { CalendarPlus, LoaderCircle, Repeat2, Trash2 } from 'lucide-react'
import { Modal } from './Modal'
import {
  formatLocalDate,
  formatRecurrence,
  fromDateInput,
  fromDateTimeInput,
  toDateInput,
  toDateTimeInput,
} from '../format'
import type { EventItem, EventPayload, Group, RecurrenceUnit } from '../types'

interface EventFormModalProps {
  event?: EventItem
  initialStart?: Date
  initialAllDay?: boolean
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

const repeatOptions: Array<{ value: RecurrenceUnit; label: string; unit: string }> = [
  { value: 'DAYS', label: 'Daily', unit: 'days' },
  { value: 'WEEKS', label: 'Weekly', unit: 'weeks' },
  { value: 'MONTHS', label: 'Monthly', unit: 'months' },
  { value: 'YEARS', label: 'Yearly', unit: 'years' },
]

export function EventFormModal({
  event,
  initialStart,
  initialAllDay = false,
  groups,
  onClose,
  onSave,
  onDelete,
}: EventFormModalProps) {
  const defaults = defaultTimes(initialStart)
  const startsAllDay = event?.allDay ?? initialAllDay
  const baseStart = event ? event.seriesStartTime ?? event.startTime : undefined
  const baseEnd = event ? event.seriesEndTime ?? event.endTime : undefined
  const initialStartValue = baseStart
    ? (startsAllDay ? toDateInput(baseStart) : toDateTimeInput(baseStart))
    : (startsAllDay ? formatLocalDate(initialStart ?? new Date()) : defaults.start)
  const initialEndValue = baseEnd
    ? (startsAllDay ? toDateInput(baseEnd) : toDateTimeInput(baseEnd))
    : (startsAllDay ? initialStartValue : defaults.end)

  const [name, setName] = useState(event?.name ?? '')
  const [description, setDescription] = useState(event?.description ?? '')
  const [allDay, setAllDay] = useState(startsAllDay)
  const [startValue, setStartValue] = useState(initialStartValue)
  const [endValue, setEndValue] = useState(initialEndValue)
  const [recurrenceUnit, setRecurrenceUnit] = useState<RecurrenceUnit | ''>(event?.recurrenceUnit ?? '')
  const [recurrenceInterval, setRecurrenceInterval] = useState(String(event?.recurrenceInterval ?? 1))
  const [recurrenceEnd, setRecurrenceEnd] = useState(event?.recurrenceEnd ? toDateInput(event.recurrenceEnd) : '')
  const [groupId, setGroupId] = useState(event?.groupId ?? '')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)

  function toggleAllDay(checked: boolean) {
    if (checked) {
      setStartValue(startValue.slice(0, 10))
      setEndValue(endValue.slice(0, 10))
    } else {
      const startDate = startValue.slice(0, 10)
      const endDate = endValue.slice(0, 10)
      setStartValue(`${startDate}T09:00`)
      setEndValue(endDate === startDate ? `${startDate}T10:00` : `${endDate}T17:00`)
    }
    setAllDay(checked)
  }

  async function submit(submitEvent: FormEvent<HTMLFormElement>) {
    submitEvent.preventDefault()
    setError('')

    const startTime = allDay ? fromDateInput(startValue) : fromDateTimeInput(startValue)
    const endTime = allDay ? `${endValue}T23:59:59` : fromDateTimeInput(endValue)
    if (new Date(endTime).getTime() < new Date(startTime).getTime()) {
      setError('The end time must be after the start time.')
      return
    }
    if (recurrenceUnit && recurrenceEnd && recurrenceEnd < startValue.slice(0, 10)) {
      setError('The repeating series cannot end before it starts.')
      return
    }
    const interval = Number(recurrenceInterval)
    if (recurrenceUnit && (!Number.isInteger(interval) || interval < 1 || interval > 1000)) {
      setError('Choose a repeat interval between 1 and 1000.')
      return
    }

    setSaving(true)
    try {
      await onSave({
        name: name.trim(),
        description: description.trim() || null,
        startTime,
        endTime,
        allDay,
        recurrenceUnit: recurrenceUnit || null,
        recurrenceInterval: recurrenceUnit ? interval : null,
        recurrenceEnd: recurrenceUnit && recurrenceEnd ? `${recurrenceEnd}T23:59:59` : null,
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
      wide
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

        <label className="checkbox-field">
          <input type="checkbox" checked={allDay} onChange={(e) => toggleAllDay(e.target.checked)} />
          <span className="checkbox-custom"><CalendarPlus size={13} /></span>
          <span>All-day event <small>No start or end time</small></span>
        </label>

        <div className="form-grid-two">
          {allDay ? (
            <>
              <label className="field">
                <span>Starts</span>
                <input type="date" required value={startValue} onChange={(e) => setStartValue(e.target.value)} />
              </label>
              <label className="field">
                <span>Ends</span>
                <input type="date" required value={endValue} onChange={(e) => setEndValue(e.target.value)} />
              </label>
            </>
          ) : (
            <>
              <label className="field">
                <span>Starts</span>
                <input type="datetime-local" required value={startValue} onChange={(e) => setStartValue(e.target.value)} />
              </label>
              <label className="field">
                <span>Ends</span>
                <input type="datetime-local" required value={endValue} onChange={(e) => setEndValue(e.target.value)} />
              </label>
            </>
          )}
        </div>

        <div className="recurrence-panel">
          <div className="recurrence-heading">
            <span className="recurrence-icon"><Repeat2 size={16} /></span>
            <div>
              <strong>Repeat this event</strong>
              <small>{formatRecurrence(recurrenceUnit || null, recurrenceUnit ? Number(recurrenceInterval) || 1 : null)}</small>
            </div>
          </div>
          <label className="field">
            <span>Frequency</span>
            <select value={recurrenceUnit} onChange={(e) => setRecurrenceUnit(e.target.value as RecurrenceUnit | '')}>
              <option value="">Does not repeat</option>
              {repeatOptions.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          {recurrenceUnit && (
            <div className="form-grid-two">
              <label className="field">
                <span>Every</span>
                <div className="repeat-interval-row">
                  <input
                    type="number"
                    min={1}
                    max={1000}
                    required
                    value={recurrenceInterval}
                    onChange={(e) => setRecurrenceInterval(e.target.value)}
                    aria-label="Repeat interval"
                  />
                  <span>{repeatOptions.find((option) => option.value === recurrenceUnit)?.unit}</span>
                </div>
              </label>
              <label className="field">
                <span>Series ends <small>Optional</small></span>
                <input type="date" value={recurrenceEnd} onChange={(e) => setRecurrenceEnd(e.target.value)} />
              </label>
            </div>
          )}
          {event?.recurrenceUnit && (
            <p className="field-hint recurrence-hint">
              This is one occurrence of a repeating event. Saving changes updates the whole series.
            </p>
          )}
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
