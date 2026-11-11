import { useCallback, useEffect, useRef, useState } from 'react'
import { CalendarDays, CheckSquare, KeyRound, LoaderCircle, Search, ShieldCheck, Trash2, Users } from 'lucide-react'
import { api } from '../api'
import { ResetPasswordModal } from '../components/ResetPasswordModal'
import { formatDate, formatEventTime, formatRecurrence, formatTime } from '../format'
import { useAutoRefresh } from '../hooks/useAutoRefresh'
import type { EventItem, PageResult, TodoItem, ToastNotice, User } from '../types'

type AdminTab = 'users' | 'events' | 'todos'

const PAGE_SIZE = 25

interface AdminPageProps {
  currentUser: User
  refreshVersion: number
  onDeleteUser: (id: string) => Promise<void>
  onNotice: (message: string, kind?: ToastNotice['kind']) => void
}

export function AdminPage({ currentUser, refreshVersion, onDeleteUser, onNotice }: AdminPageProps) {
  const [tab, setTab] = useState<AdminTab>('users')
  const [users, setUsers] = useState<PageResult<User> | null>(null)
  const [events, setEvents] = useState<PageResult<EventItem> | null>(null)
  const [todos, setTodos] = useState<PageResult<TodoItem> | null>(null)
  const [userPage, setUserPage] = useState(0)
  const [eventPage, setEventPage] = useState(0)
  const [todoPage, setTodoPage] = useState(0)
  const [search, setSearch] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [resetUser, setResetUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const loadedRef = useRef(false)

  const loadAdmin = useCallback(async (background = false) => {
    if (!loadedRef.current) setLoading(true)
    const searchTerm = search.trim() || undefined
    try {
      const [userData, eventData, todoData] = await Promise.all([
        api.adminListUsers({ page: userPage, size: PAGE_SIZE, search: searchTerm }),
        api.adminListEvents({
          page: eventPage,
          size: PAGE_SIZE,
          search: searchTerm,
          from: fromDate || undefined,
          to: toDate || undefined,
        }),
        api.adminListTodos({ page: todoPage, size: PAGE_SIZE, search: searchTerm }),
      ])
      setUsers(userData)
      setEvents(eventData)
      setTodos(todoData)
      loadedRef.current = true
    } catch (error) {
      if (!background) onNotice(error instanceof Error ? error.message : 'Could not load administration data.', 'error')
    } finally {
      setLoading(false)
    }
  }, [eventPage, fromDate, onNotice, search, toDate, todoPage, userPage])

  useEffect(() => {
    void loadAdmin()
  }, [loadAdmin, refreshVersion])

  useAutoRefresh(() => loadAdmin(true))

  const userItems = users?.items ?? []
  const eventItems = events?.items ?? []
  const todoItems = todos?.items ?? []

  function changeSearch(value: string) {
    setSearch(value)
    setUserPage(0)
    setEventPage(0)
    setTodoPage(0)
  }

  function changeFromDate(value: string) {
    setFromDate(value)
    setEventPage(0)
  }

  function changeToDate(value: string) {
    setToDate(value)
    setEventPage(0)
  }

  async function deleteUser(user: User) {
    if (user.id === currentUser.id) {
      onNotice('You cannot delete the account you are currently using.', 'info')
      return
    }
    if (!window.confirm(`Delete ${user.login}? Their todos and events will also be deleted.`)) return
    try {
      await onDeleteUser(user.id)
      onNotice(`${user.login} and their data were deleted.`)
    } catch (error) {
      onNotice(error instanceof Error ? error.message : 'Could not delete this user.', 'error')
    }
  }

  async function resetPassword(newPassword: string) {
    if (!resetUser) return
    await api.adminResetPassword(resetUser.id, newPassword)
    onNotice(`Password reset for ${resetUser.login}. Their sessions were signed out.`, 'info')
    setResetUser(null)
  }

  return (
    <div className="page-content admin-page">
      <div className="page-heading">
        <div><span className="eyebrow">ADMINISTRATION</span><h1>All accounts, in one place</h1><p>Manage users and keep an eye on shared dashboard data.</p></div>
        <span className="admin-heading-badge"><ShieldCheck size={16} /> ADMIN ACCESS</span>
      </div>

      <div className="admin-stat-row">
        <div className="admin-stat surface-card"><span className="stat-icon stat-icon-green"><Users size={18} /></span><span><small>ACCOUNTS</small><strong>{loading ? '—' : users?.total ?? 0}</strong></span></div>
        <div className="admin-stat surface-card"><span className="stat-icon stat-icon-sand"><CalendarDays size={18} /></span><span><small>EVENTS</small><strong>{loading ? '—' : events?.total ?? 0}</strong></span></div>
        <div className="admin-stat surface-card"><span className="stat-icon stat-icon-lilac"><CheckSquare size={18} /></span><span><small>TASKS</small><strong>{loading ? '—' : todos?.total ?? 0}</strong></span></div>
      </div>

      <section className="surface-card admin-board">
        <div className="admin-toolbar">
          <div className="admin-tabs" role="tablist" aria-label="Admin data">
            <button className={tab === 'users' ? 'admin-tab active' : 'admin-tab'} onClick={() => setTab('users')} role="tab" aria-selected={tab === 'users'}><Users size={15} /> Users <span>{users?.total ?? 0}</span></button>
            <button className={tab === 'events' ? 'admin-tab active' : 'admin-tab'} onClick={() => setTab('events')} role="tab" aria-selected={tab === 'events'}><CalendarDays size={15} /> Events <span>{events?.total ?? 0}</span></button>
            <button className={tab === 'todos' ? 'admin-tab active' : 'admin-tab'} onClick={() => setTab('todos')} role="tab" aria-selected={tab === 'todos'}><CheckSquare size={15} /> To-dos <span>{todos?.total ?? 0}</span></button>
          </div>
          <label className="search-field admin-search"><Search size={16} /><input value={search} onChange={(e) => changeSearch(e.target.value)} placeholder={tab === 'users' ? 'Search users' : `Search ${tab}`} /></label>
        </div>

        {tab === 'events' && <div className="admin-date-filters"><span>Filter by event date</span><label><span>From</span><input type="date" value={fromDate} onChange={(e) => changeFromDate(e.target.value)} /></label><label><span>To</span><input type="date" value={toDate} onChange={(e) => changeToDate(e.target.value)} /></label><small>Search, date filters, and paging run on the server.</small></div>}

        {loading ? (
          <div className="admin-loading"><LoaderCircle className="spin" size={19} /> Loading administration data…</div>
        ) : tab === 'users' ? (
          <div>
            <div className="table-scroll"><table className="data-table"><thead><tr><th>USER</th><th>ROLE</th><th>JOINED</th><th>USER ID</th><th /></tr></thead><tbody>
              {userItems.map((user) => <tr key={user.id}><td><div className="table-user"><span className="table-avatar">{user.login.slice(0, 1).toUpperCase()}</span><strong>{user.login}{user.id === currentUser.id && <small className="you-label">YOU</small>}</strong></div></td><td><span className={`role-badge role-badge-small ${user.role === 'ADMIN' ? 'role-admin' : 'role-user'}`}>{user.role === 'ADMIN' ? <ShieldCheck size={12} /> : <Users size={12} />}{user.role}</span></td><td>{formatDate(user.creationTime)}</td><td><code>{user.id.slice(0, 8)}…</code></td><td className="table-actions"><button className="icon-button" onClick={() => setResetUser(user)} aria-label={`Reset password for ${user.login}`} title="Reset password"><KeyRound size={16} /></button>{user.id !== currentUser.id && <button className="icon-button delete-user-button" onClick={() => void deleteUser(user)} aria-label={`Delete ${user.login}`} title="Delete user"><Trash2 size={16} /></button>}</td></tr>)}
            </tbody></table>{userItems.length === 0 && <EmptyAdmin label="No users match that search." />}</div>
            <Pagination data={users} onPageChange={setUserPage} />
          </div>
        ) : tab === 'events' ? (
          <div>
            <div className="table-scroll"><table className="data-table"><thead><tr><th>EVENT</th><th>OWNER</th><th>STARTS</th><th>ENDS</th><th>DESCRIPTION</th></tr></thead><tbody>
              {eventItems.map((event) => <tr key={event.id}><td><strong>{event.name}</strong>{event.recurrenceUnit && <small className="table-subline">{formatRecurrence(event.recurrenceUnit, event.recurrenceInterval)}</small>}</td><td>{event.userLogin}</td><td><span>{formatDate(event.startTime)}</span><small className="table-subline">{formatEventTime(event)}</small></td><td><span>{formatDate(event.endTime)}</span><small className="table-subline">{event.allDay ? 'All day' : formatTime(event.endTime)}</small></td><td className="table-description">{event.description || '—'}</td></tr>)}
            </tbody></table>{eventItems.length === 0 && <EmptyAdmin label="No events match these filters." />}</div>
            <Pagination data={events} onPageChange={setEventPage} />
          </div>
        ) : (
          <div>
            <div className="table-scroll"><table className="data-table"><thead><tr><th>TASK</th><th>OWNER</th><th>STATUS</th><th>ADDED</th><th>DESCRIPTION</th></tr></thead><tbody>
              {todoItems.map((todo) => <tr key={todo.id}><td><strong>{todo.name}</strong></td><td>{todo.userLogin}</td><td><span className={`status-pill ${todo.completed ? 'status-done' : 'status-open'}`}>{todo.completed ? 'Completed' : 'Open'}</span></td><td>{formatDate(todo.creationTime)}</td><td className="table-description">{todo.description || '—'}</td></tr>)}
            </tbody></table>{todoItems.length === 0 && <EmptyAdmin label="No tasks match that search." />}</div>
            <Pagination data={todos} onPageChange={setTodoPage} />
          </div>
        )}
      </section>

      {resetUser && (
        <ResetPasswordModal user={resetUser} onClose={() => setResetUser(null)} onSave={resetPassword} />
      )}
    </div>
  )
}

function Pagination<T>({ data, onPageChange }: { data: PageResult<T> | null; onPageChange: (page: number) => void }) {
  if (!data || data.total === 0) return null
  return (
    <div className="admin-pagination">
      <button className="button button-secondary button-small" type="button" disabled={data.page <= 0} onClick={() => onPageChange(data.page - 1)}>Previous</button>
      <span>Page {data.page + 1} of {data.totalPages} · {data.total} total</span>
      <button className="button button-secondary button-small" type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => onPageChange(data.page + 1)}>Next</button>
    </div>
  )
}

function EmptyAdmin({ label }: { label: string }) {
  return <div className="admin-empty">{label}</div>
}
