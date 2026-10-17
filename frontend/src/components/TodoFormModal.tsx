import { useState, type FormEvent } from 'react'
import { Check, LoaderCircle, Trash2 } from 'lucide-react'
import { Modal } from './Modal'
import type { TodoItem, TodoPayload } from '../types'

interface TodoFormModalProps {
  todo?: TodoItem
  onClose: () => void
  onSave: (payload: TodoPayload) => Promise<void>
  onDelete?: () => Promise<void>
}

export function TodoFormModal({ todo, onClose, onSave, onDelete }: TodoFormModalProps) {
  const [name, setName] = useState(todo?.name ?? '')
  const [description, setDescription] = useState(todo?.description ?? '')
  const [completed, setCompleted] = useState(todo?.completed ?? false)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      await onSave({ name: name.trim(), description: description.trim() || null, completed })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not save this task.')
    } finally {
      setSaving(false)
    }
  }

  async function removeTodo() {
    if (!onDelete) return
    setDeleting(true)
    setError('')
    try {
      await onDelete()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not delete this task.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <Modal
      title={todo ? 'Edit task' : 'Add a task'}
      eyebrow={todo ? 'Your to-do list' : 'Make a little progress'}
      onClose={onClose}
      footer={
        <>
          {todo && onDelete && (
            <button className="button button-danger-quiet" type="button" onClick={removeTodo} disabled={deleting || saving}>
              {deleting ? <LoaderCircle className="spin" size={16} /> : <Trash2 size={16} />}
              Delete
            </button>
          )}
          <span className="modal-footer-spacer" />
          <button className="button button-secondary" type="button" onClick={onClose} disabled={saving || deleting}>
            Cancel
          </button>
          <button className="button button-primary" type="submit" form="todo-form" disabled={saving || deleting}>
            {saving ? <LoaderCircle className="spin" size={16} /> : <Check size={16} />}
            {todo ? 'Save changes' : 'Add task'}
          </button>
        </>
      }
    >
      <form id="todo-form" className="form-stack" onSubmit={submit}>
        <label className="field">
          <span>Task</span>
          <input autoFocus required maxLength={255} value={name} onChange={(e) => setName(e.target.value)} placeholder="What needs doing?" />
        </label>
        <label className="field">
          <span>Notes <small>Optional</small></span>
          <textarea rows={4} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Add a helpful note" />
        </label>
        <label className="checkbox-field">
          <input type="checkbox" checked={completed} onChange={(e) => setCompleted(e.target.checked)} />
          <span className="checkbox-custom"><Check size={13} /></span>
          <span>Mark as completed</span>
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>
    </Modal>
  )
}
