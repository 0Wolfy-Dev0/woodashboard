import { useState, type FormEvent } from 'react'
import { Check, Folder, FolderPlus, LoaderCircle, Pencil, Trash2, X } from 'lucide-react'
import { api } from '../api'
import { Modal } from './Modal'
import type { TodoFolder, ToastNotice } from '../types'

interface TodoFoldersModalProps {
  folders: TodoFolder[]
  onClose: () => void
  onChanged: () => Promise<void>
  onNotice: (message: string, kind?: ToastNotice['kind']) => void
}

export function TodoFoldersModal({ folders, onClose, onChanged, onNotice }: TodoFoldersModalProps) {
  const [newName, setNewName] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editingName, setEditingName] = useState('')
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

  async function createFolder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const name = newName.trim()
    if (!name) return
    setError('')
    setCreating(true)
    try {
      await api.createTodoFolder({ name })
      setNewName('')
      await onChanged()
      onNotice('Folder created.')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not create this folder.')
    } finally {
      setCreating(false)
    }
  }

  function startRename(folder: TodoFolder) {
    setError('')
    setEditingId(folder.id)
    setEditingName(folder.name)
  }

  async function renameFolder(folder: TodoFolder) {
    const name = editingName.trim()
    if (!name || name === folder.name) {
      setEditingId(null)
      return
    }
    setError('')
    setBusyId(folder.id)
    try {
      await api.updateTodoFolder(folder.id, { name })
      setEditingId(null)
      await onChanged()
      onNotice('Folder renamed.')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not rename this folder.')
    } finally {
      setBusyId(null)
    }
  }

  async function deleteFolder(folder: TodoFolder) {
    if (!window.confirm(`Delete the folder "${folder.name}"? Its tasks are kept and become unfiled.`)) return
    setError('')
    setBusyId(folder.id)
    try {
      await api.deleteTodoFolder(folder.id)
      if (editingId === folder.id) setEditingId(null)
      await onChanged()
      onNotice(`Folder "${folder.name}" deleted; its tasks are now unfiled.`, 'info')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not delete this folder.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <Modal
      title="Todo folders"
      eyebrow="Keep your lists tidy"
      onClose={onClose}
      footer={
        <>
          <span className="modal-footer-spacer" />
          <button className="button button-secondary" type="button" onClick={onClose}>Done</button>
        </>
      }
    >
      <div className="folder-manager">
        <p className="folder-manager-note">Folders are personal. Shared tasks stay shared when you move them into a folder.</p>

        <form className="folder-create-form" onSubmit={createFolder}>
          <label className="search-field folder-create-field">
            <FolderPlus size={16} />
            <input
              value={newName}
              onChange={(event) => setNewName(event.target.value)}
              maxLength={100}
              placeholder="New folder name"
              aria-label="New folder name"
            />
          </label>
          <button className="button button-primary button-small" type="submit" disabled={creating || !newName.trim()}>
            {creating ? <LoaderCircle className="spin" size={14} /> : <FolderPlus size={14} />}
            Create
          </button>
        </form>

        {folders.length === 0 ? (
          <div className="empty-state compact-empty folder-manager-empty">
            <span className="empty-icon"><Folder size={19} /></span>
            <strong>No folders yet</strong>
            <p>Create one to separate errands, home, and work.</p>
          </div>
        ) : (
          <div className="folder-manager-list">
            {folders.map((folder) => (
              <div className="folder-manager-row" key={folder.id}>
                <span className="folder-manager-icon"><Folder size={15} /></span>
                {editingId === folder.id ? (
                  <input
                    className="folder-rename-input"
                    autoFocus
                    maxLength={100}
                    value={editingName}
                    onChange={(event) => setEditingName(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') void renameFolder(folder)
                      if (event.key === 'Escape') setEditingId(null)
                    }}
                    aria-label={`Rename ${folder.name}`}
                  />
                ) : (
                  <strong>{folder.name}</strong>
                )}
                <span className="folder-manager-actions">
                  {editingId === folder.id ? (
                    <>
                      <button className="icon-button folder-icon-button" type="button" onClick={() => void renameFolder(folder)} disabled={busyId === folder.id} title="Save name" aria-label="Save name">
                        {busyId === folder.id ? <LoaderCircle className="spin" size={15} /> : <Check size={15} />}
                      </button>
                      <button className="icon-button folder-icon-button" type="button" onClick={() => setEditingId(null)} title="Cancel rename" aria-label="Cancel rename"><X size={15} /></button>
                    </>
                  ) : (
                    <>
                      <button className="icon-button folder-icon-button" type="button" onClick={() => startRename(folder)} title="Rename folder" aria-label={`Rename ${folder.name}`}><Pencil size={14} /></button>
                      <button className="icon-button folder-icon-button folder-delete-button" type="button" onClick={() => void deleteFolder(folder)} disabled={busyId === folder.id} title="Delete folder" aria-label={`Delete ${folder.name}`}>
                        {busyId === folder.id ? <LoaderCircle className="spin" size={14} /> : <Trash2 size={14} />}
                      </button>
                    </>
                  )}
                </span>
              </div>
            ))}
          </div>
        )}

        {error && <p className="form-error" role="alert">{error}</p>}
      </div>
    </Modal>
  )
}
