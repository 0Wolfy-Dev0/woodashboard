import { useEffect, useMemo, useState, type FormEvent } from 'react'
import {
  Crown,
  LoaderCircle,
  LogOut,
  Pencil,
  Plus,
  Sparkles,
  Trash2,
  UserPlus,
  Users,
} from 'lucide-react'
import { api } from '../api'
import { GroupFormModal } from '../components/GroupFormModal'
import { formatDate } from '../format'
import type { Group, GroupPayload, ToastNotice, User } from '../types'

interface GroupsPageProps {
  groups: Group[]
  currentUser: User
  onGroupsChanged: () => Promise<void>
  onNotice: (message: string, kind?: ToastNotice['kind']) => void
}

export function GroupsPage({ groups, currentUser, onGroupsChanged, onNotice }: GroupsPageProps) {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [dialog, setDialog] = useState<{ group?: Group } | null>(null)
  const [memberLogin, setMemberLogin] = useState('')
  const [memberError, setMemberError] = useState('')
  const [addingMember, setAddingMember] = useState(false)
  const [removingId, setRemovingId] = useState<string | null>(null)

  const selectedGroup = useMemo(
    () => groups.find((group) => group.id === selectedId) ?? groups[0] ?? null,
    [groups, selectedId],
  )

  useEffect(() => {
    if (!groups.some((group) => group.id === selectedId)) {
      setSelectedId(groups[0]?.id ?? null)
    }
  }, [groups, selectedId])

  const canManage = selectedGroup
    ? currentUser.role === 'ADMIN' || selectedGroup.ownerId === currentUser.id
    : false

  async function saveGroup(payload: GroupPayload) {
    const editing = Boolean(dialog?.group)
    const saved = dialog?.group
      ? await api.updateGroup(dialog.group.id, payload)
      : await api.createGroup(payload)
    await onGroupsChanged()
    setSelectedId(saved.id)
    setDialog(null)
    onNotice(editing ? 'Group updated.' : 'Group created.')
  }

  async function deleteGroup() {
    if (!selectedGroup) return
    if (!window.confirm(`Delete "${selectedGroup.name}"? Shared events and tasks will also be deleted.`)) return
    try {
      await api.deleteGroup(selectedGroup.id)
      setSelectedId(null)
      await onGroupsChanged()
      onNotice(`${selectedGroup.name} was deleted.`, 'info')
    } catch (error) {
      onNotice(error instanceof Error ? error.message : 'Could not delete this group.', 'error')
    }
  }

  async function addMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedGroup) return
    setMemberError('')
    setAddingMember(true)
    try {
      await api.addGroupMember(selectedGroup.id, memberLogin.trim())
      setMemberLogin('')
      await onGroupsChanged()
      onNotice('Member added to the group.')
    } catch (error) {
      setMemberError(error instanceof Error ? error.message : 'Could not add this member.')
    } finally {
      setAddingMember(false)
    }
  }

  async function removeMember(member: User) {
    if (!selectedGroup) return
    const isSelf = member.id === currentUser.id
    if (!isSelf && !window.confirm(`Remove ${member.login} from ${selectedGroup.name}?`)) return
    setRemovingId(member.id)
    try {
      await api.removeGroupMember(selectedGroup.id, member.id)
      if (isSelf) setSelectedId(null)
      await onGroupsChanged()
      onNotice(isSelf ? `You left ${selectedGroup.name}.` : `${member.login} was removed.`, 'info')
    } catch (error) {
      onNotice(error instanceof Error ? error.message : 'Could not remove this member.', 'error')
    } finally {
      setRemovingId(null)
    }
  }

  return (
    <div className="page-content groups-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">SHARE THE LITTLE THINGS</span>
          <h1>Groups</h1>
          <p>Bring people together around shared plans and tasks.</p>
        </div>
        <button className="button button-primary" onClick={() => setDialog({})}><Plus size={17} /> New group</button>
      </div>

      {groups.length === 0 ? (
        <section className="surface-card group-empty">
          <span className="group-empty-icon"><Users size={25} /></span>
          <span className="eyebrow">YOUR PEOPLE, TOGETHER</span>
          <h2>Make your first group</h2>
          <p>Share events and to-dos with family, friends, or your home lab crew.</p>
          <button className="button button-primary" onClick={() => setDialog({})}><Plus size={16} /> Create a group</button>
        </section>
      ) : (
        <div className="groups-layout">
          <section className="surface-card group-list-card">
            <div className="section-heading section-heading-small">
              <div><span className="eyebrow">YOUR CIRCLES</span><h2>{groups.length} {groups.length === 1 ? 'group' : 'groups'}</h2></div>
              <span className="aside-heading-icon"><Users size={17} /></span>
            </div>
            <div className="group-list">
              {groups.map((group) => (
                <button
                  className={`group-list-item${selectedGroup?.id === group.id ? ' active' : ''}`}
                  key={group.id}
                  onClick={() => setSelectedId(group.id)}
                >
                  <span className="group-avatar">{group.name.slice(0, 1).toUpperCase()}</span>
                  <span className="group-list-copy">
                    <strong>{group.name}</strong>
                    <small>{group.members.length} {group.members.length === 1 ? 'member' : 'members'} · {group.ownerLogin}</small>
                  </span>
                  <span className="group-list-arrow">›</span>
                </button>
              ))}
            </div>
          </section>

          {selectedGroup && (
            <section className="surface-card group-detail-card">
              <header className="group-detail-header">
                <div className="group-detail-identity">
                  <span className="group-detail-avatar">{selectedGroup.name.slice(0, 1).toUpperCase()}</span>
                  <div>
                    <span className="eyebrow">GROUP SPACE</span>
                    <h2>{selectedGroup.name}</h2>
                    <p>{selectedGroup.description || 'A shared space for the people in your circle.'}</p>
                  </div>
                </div>
                {canManage && (
                  <div className="group-actions">
                    <button className="button button-secondary button-small" onClick={() => setDialog({ group: selectedGroup })}><Pencil size={14} /> Edit</button>
                    <button className="icon-button delete-user-button" onClick={() => void deleteGroup()} title="Delete group" aria-label="Delete group"><Trash2 size={16} /></button>
                  </div>
                )}
              </header>

              <div className="group-meta">
                <span><Crown size={14} /> Owned by <strong>{selectedGroup.ownerLogin}</strong></span>
                <span>Created {formatDate(selectedGroup.creationTime)}</span>
              </div>

              <div className="group-members-heading">
                <div><span className="eyebrow">THE PEOPLE IN THIS SPACE</span><h3>{selectedGroup.members.length} {selectedGroup.members.length === 1 ? 'member' : 'members'}</h3></div>
                {canManage && (
                  <form className="group-add-member" onSubmit={addMember}>
                    <UserPlus size={15} />
                    <input value={memberLogin} onChange={(e) => setMemberLogin(e.target.value)} placeholder="Add member by login" aria-label="Member login" />
                    <button className="button button-secondary button-small" type="submit" disabled={addingMember || !memberLogin.trim()}>
                      {addingMember ? <LoaderCircle className="spin" size={14} /> : 'Add'}
                    </button>
                  </form>
                )}
              </div>
              {memberError && <p className="form-error group-member-error" role="alert">{memberError}</p>}

              <div className="group-members">
                {selectedGroup.members.map((member) => {
                  const isOwner = member.id === selectedGroup.ownerId
                  const isSelf = member.id === currentUser.id
                  const canRemove = !isOwner && (canManage || isSelf)
                  return (
                    <div className="group-member-row" key={member.id}>
                      <span className="group-member-avatar">{member.login.slice(0, 1).toUpperCase()}</span>
                      <span className="group-member-copy"><strong>{member.login}{isSelf && <small className="you-label">YOU</small>}</strong><small>{isOwner ? 'Group owner' : member.role === 'ADMIN' ? 'Administrator' : 'Member'}</small></span>
                      {isOwner && <span className="group-owner-badge"><Crown size={12} /> Owner</span>}
                      {canRemove && (
                        <button className="button button-danger-quiet button-small group-member-remove" onClick={() => void removeMember(member)} disabled={removingId === member.id}>
                          {removingId === member.id ? <LoaderCircle className="spin" size={14} /> : isSelf ? <LogOut size={14} /> : <Trash2 size={14} />}
                          {isSelf ? 'Leave' : 'Remove'}
                        </button>
                      )}
                    </div>
                  )
                })}
              </div>

              <div className="group-detail-footer"><Sparkles size={15} /> Shared events and tasks appear in your calendar and task list.</div>
            </section>
          )}
        </div>
      )}

      {dialog && <GroupFormModal group={dialog.group} onClose={() => setDialog(null)} onSave={saveGroup} />}
    </div>
  )
}
