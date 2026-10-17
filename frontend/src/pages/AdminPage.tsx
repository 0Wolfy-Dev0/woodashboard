import { useEffect, useMemo, useState } from 'react'
import { CalendarDays, CheckSquare, LoaderCircle, Search, ShieldCheck, Trash2, Users } from 'lucide-react'
import { api } from '../api'
import { formatDate, formatTime } from '../format'
import type { EventItem, TodoItem, ToastNotice, User } from '../types'

type AdminTab = 'users' | 'events' | 'todos'

interface AdminPageProps {
  currentUser: User
  refreshVersion: number
  onDeleteUser: (id: string) => Promise<void>
  onNotice: (message: string, kind?: ToastNotice['kind']) => void
}

export function AdminPage({ currentUser, refreshVersion, onDeleteUser, onNotice }: AdminPageProps) {
  const [tab, setTab] = useState<AdminTab>('users')
  const [users, setUsers] = useState<User[]>([])
  const [events, setEvents] = useState<EventItem[]>([])
  const [todos, setTodos] = useState<TodoItem[]>([])
  const [search, setSearch] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    Promise.all([api.adminListUsers(), api.adminListEvents(), api.adminListTodos()])
      .then(([allUsers, allEvents, allTodos]) => {
        if (!active) return
        setUsers(allUsers)
        setEvents(allEvents)
        setTodos(allTodos)
      })
      .catch((error: unknown) => {
        if (active) onNotice(error instanceof Error ? error.message : 'Could not load administration data.', 'error')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [refreshVersion, onNotice])

  const userNames = useMemo(() => new Map(users.map((user) => [user.id, user.login])), [users])
  const normalizedSearch = search.trim().toLocaleLowerCase()
  const visibleUsers = users.filter((user) => `${user.login} ${user.role}`.toLocaleLowerCase().includes(normalizedSearch))
  const visibleEvents = events.filter((event) => {
    const matchesText = `${event.name} ${event.description ?? ''} ${userNames.get(event.userId) ?? ''}`.toLocaleLowerCase().includes(normalizedSearch)
    const eventDate = event.startTime.slice(0, 10)
    return matchesText && (!fromDate || eventDate >= fromDate) && (!toDate || eventDate <= toDate)
  })
  const visibleTodos = todos.filter((todo) => `${todo.name} ${todo.description ?? ''} ${userNames.get(todo.userId) ?? ''}`.toLocaleLowerCase().includes(normalizedSearch))

  async function deleteUser(user: User) {
    if (user.id === currentUser.id) {
      onNotice('You cannot delete the account you are currently using.', 'info')
      return
    }
    if (!window.confirm(`Delete ${user.login}? Their todos and events will also be deleted.`)) return
    try {
      await onDeleteUser(user.id)
      setUsers((items) => items.filter((item) => item.id !== user.id))
      setEvents((items) => items.filter((item) => item.userId !== user.id))
      setTodos((items) => items.filter((item) => item.userId !== user.id))
      onNotice(`${user.login} and their data were deleted.`)
    } catch (error) {
      onNotice(error instanceof Error ? error.message : 'Could not delete this user.', 'error')
    }
  }

  return (
    <div className="page-content admin-page">
      <div className="page-heading">
        <div><span className="eyebrow">ADMINISTRATION</span><h1>All accounts, in one place</h1><p>Manage users and keep an eye on shared dashboard data.</p></div>
        <span className="admin-heading-badge"><ShieldCheck size={16} /> ADMIN ACCESS</span>
      </div>

      <div className="admin-stat-row">
        <div className="admin-stat surface-card"><span className="stat-icon stat-icon-green"><Users size={18} /></span><span><small>ACCOUNTS</small><strong>{loading ? '—' : users.length}</strong></span></div>
        <div className="admin-stat surface-card"><span className="stat-icon stat-icon-sand"><CalendarDays size={18} /></span><span><small>EVENTS</small><strong>{loading ? '—' : events.length}</strong></span></div>
        <div className="admin-stat surface-card"><span className="stat-icon stat-icon-lilac"><CheckSquare size={18} /></span><span><small>TASKS</small><strong>{loading ? '—' : todos.length}</strong></span></div>
      </div>

      <section className="surface-card admin-board">
        <div className="admin-toolbar">
          <div className="admin-tabs" role="tablist" aria-label="Admin data">
            <button className={tab === 'users' ? 'admin-tab active' : 'admin-tab'} onClick={() => setTab('users')} role="tab" aria-selected={tab === 'users'}><Users size={15} /> Users <span>{users.length}</span></button>
            <button className={tab === 'events' ? 'admin-tab active' : 'admin-tab'} onClick={() => setTab('events')} role="tab" aria-selected={tab === 'events'}><CalendarDays size={15} /> Events <span>{events.length}</span></button>
            <button className={tab === 'todos' ? 'admin-tab active' : 'admin-tab'} onClick={() => setTab('todos')} role="tab" aria-selected={tab === 'todos'}><CheckSquare size={15} /> To-dos <span>{todos.length}</span></button>
          </div>
          <label className="search-field admin-search"><Search size={16} /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={tab === 'users' ? 'Search users' : `Search ${tab}`} /></label>
        </div>

        {tab === 'events' && <div className="admin-date-filters"><span>Filter by event date</span><label><span>From</span><input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} /></label><label><span>To</span><input type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} /></label><small>Admin endpoints return all records; these date filters are applied here.</small></div>}

        {loading ? (
          <div className="admin-loading"><LoaderCircle className="spin" size={19} /> Loading administration data…</div>
        ) : tab === 'users' ? (
          <div className="table-scroll"><table className="data-table"><thead><tr><th>USER</th><th>ROLE</th><th>JOINED</th><th>USER ID</th><th /></tr></thead><tbody>
            {visibleUsers.map((user) => <tr key={user.id}><td><div className="table-user"><span className="table-avatar">{user.login.slice(0, 1).toUpperCase()}</span><strong>{user.login}{user.id === currentUser.id && <small className="you-label">YOU</small>}</strong></div></td><td><span className={`role-badge role-badge-small ${user.role === 'ADMIN' ? 'role-admin' : 'role-user'}`}>{user.role === 'ADMIN' ? <ShieldCheck size={12} /> : <Users size={12} />}{user.role}</span></td><td>{formatDate(user.creationTime)}</td><td><code>{user.id.slice(0, 8)}…</code></td><td className="table-actions">{user.id !== currentUser.id && <button className="icon-button delete-user-button" onClick={() => void deleteUser(user)} aria-label={`Delete ${user.login}`} title="Delete user"><Trash2 size={16} /></button>}</td></tr>)}
          </tbody></table>{visibleUsers.length === 0 && <EmptyAdmin label="No users match that search." />}</div>
        ) : tab === 'events' ? (
          <div className="table-scroll"><table className="data-table"><thead><tr><th>EVENT</th><th>OWNER</th><th>STARTS</th><th>ENDS</th><th>DESCRIPTION</th></tr></thead><tbody>
            {visibleEvents.map((event) => <tr key={event.id}><td><strong>{event.name}</strong></td><td>{userNames.get(event.userId) ?? 'Unknown user'}</td><td><span>{formatDate(event.startTime)}</span><small className="table-subline">{formatTime(event.startTime)}</small></td><td><span>{formatDate(event.endTime)}</span><small className="table-subline">{formatTime(event.endTime)}</small></td><td className="table-description">{event.description || '—'}</td></tr>)}
          </tbody></table>{visibleEvents.length === 0 && <EmptyAdmin label="No events match these filters." />}</div>
        ) : (
          <div className="table-scroll"><table className="data-table"><thead><tr><th>TASK</th><th>OWNER</th><th>STATUS</th><th>ADDED</th><th>DESCRIPTION</th></tr></thead><tbody>
            {visibleTodos.map((todo) => <tr key={todo.id}><td><strong>{todo.name}</strong></td><td>{userNames.get(todo.userId) ?? 'Unknown user'}</td><td><span className={`status-pill ${todo.completed ? 'status-done' : 'status-open'}`}>{todo.completed ? 'Completed' : 'Open'}</span></td><td>{formatDate(todo.creationTime)}</td><td className="table-description">{todo.description || '—'}</td></tr>)}
          </tbody></table>{visibleTodos.length === 0 && <EmptyAdmin label="No tasks match that search." />}</div>
        )}
      </section>
    </div>
  )
}

function EmptyAdmin({ label }: { label: string }) {
  return <div className="admin-empty">{label}</div>
}
