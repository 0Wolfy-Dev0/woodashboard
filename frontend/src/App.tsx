import { lazy, Suspense, useCallback, useEffect, useState } from 'react'
import {
  CalendarDays,
  CheckSquare,
  CircleUserRound,
  LayoutDashboard,
  LogOut,
  Menu,
  Monitor,
  PanelLeftClose,
  PanelLeftOpen,
  Plus,
  ShieldCheck,
  Sparkles,
  Users,
  X,
} from 'lucide-react'
import { api, SESSION_EXPIRED_EVENT } from './api'
import { EventFormModal } from './components/EventFormModal'
import { LoginPage } from './components/LoginPage'
import { TodoFormModal } from './components/TodoFormModal'
import { Toasts } from './components/Toasts'
import { useAutoRefresh } from './hooks/useAutoRefresh'
import { clearSession, readSession, saveSession, SESSION_STORAGE_KEY } from './session'
import { AdminPage } from './pages/AdminPage'
import { DashboardPage } from './pages/DashboardPage'
import { GroupsPage } from './pages/GroupsPage'
import { ProfilePage } from './pages/ProfilePage'
import { TodosPage } from './pages/TodosPage'
import type { EventItem, EventPayload, Group, Page, Session, TodoFolder, TodoItem, TodoPayload, ToastNotice, User } from './types'

const CalendarPage = lazy(() => import('./pages/CalendarPage').then((module) => ({ default: module.CalendarPage })))
const DisplayPage = lazy(() => import('./pages/DisplayPage').then((module) => ({ default: module.DisplayPage })))

const DISPLAY_PATH = '/display'
const isDisplayRoute = window.location.pathname.replace(/\/+$/, '') === DISPLAY_PATH

type EventDialog = { event?: EventItem; start?: Date; allDay?: boolean } | null
type TodoDialog = { todo?: TodoItem } | null

const SIDEBAR_COLLAPSED_KEY = 'woodashboard.sidebar-collapsed'

function readSidebarCollapsed(): boolean {
  try {
    return localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true'
  } catch {
    return false
  }
}

export default function App() {
  const [session, setSession] = useState<Session | null>(() => readSession())
  const [user, setUser] = useState<User | null>(null)
  const [loadingUser, setLoadingUser] = useState(Boolean(readSession()))
  const [page, setPage] = useState<Page>('home')
  const [sidebarCollapsed, setSidebarCollapsed] = useState(readSidebarCollapsed)
  const [calendarFocus, setCalendarFocus] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [eventDialog, setEventDialog] = useState<EventDialog>(null)
  const [todoDialog, setTodoDialog] = useState<TodoDialog>(null)
  const [groups, setGroups] = useState<Group[]>([])
  const [folders, setFolders] = useState<TodoFolder[]>([])
  const [refreshVersion, setRefreshVersion] = useState(0)
  const [notices, setNotices] = useState<ToastNotice[]>([])

  const notify = useCallback((message: string, kind: ToastNotice['kind'] = 'success') => {
    const id = Date.now() + Math.random()
    setNotices((items) => [...items, { id, message, kind }].slice(-4))
    window.setTimeout(() => setNotices((items) => items.filter((item) => item.id !== id)), 4500)
  }, [])

  const loadGroups = useCallback(async (silent = false) => {
    if (!user) {
      setGroups([])
      return
    }
    try {
      const first = await api.listGroups({ page: 0, size: 100 })
      const all = [...first.items]
      for (let page = 1; page < first.totalPages; page += 1) {
        const next = await api.listGroups({ page, size: 100 })
        all.push(...next.items)
      }
      setGroups(all)
    } catch (error) {
      if (!silent) notify(error instanceof Error ? error.message : 'Could not load your groups.', 'error')
    }
  }, [user?.id, notify])

  const loadFolders = useCallback(async (silent = false) => {
    if (!user) {
      setFolders([])
      return
    }
    try {
      setFolders(await api.listTodoFolders())
    } catch (error) {
      if (!silent) notify(error instanceof Error ? error.message : 'Could not load your todo folders.', 'error')
    }
  }, [user?.id, notify])

  useEffect(() => {
    void loadGroups()
  }, [loadGroups])

  useEffect(() => {
    void loadFolders()
  }, [loadFolders])

  useAutoRefresh(() => loadGroups(true))
  useAutoRefresh(() => loadFolders(true))

  useEffect(() => {
    const onSessionExpired = () => {
      clearSession()
      setSession(null)
      setUser(null)
      setPage('home')
      setLoadingUser(false)
      notify('Your session expired. Please sign in again.', 'info')
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, onSessionExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onSessionExpired)
  }, [notify])

  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key !== SESSION_STORAGE_KEY && event.key !== null) return
      const next = readSession()
      if (!next) {
        setSession(null)
        setUser(null)
        setPage('home')
        setGroups([])
        setFolders([])
        setLoadingUser(false)
        notify('You were signed out in another tab.', 'info')
        return
      }
      if (next.userId === user?.id) {
        // Another tab rotated the tokens for the same account; keep this tab's view.
        setSession(next)
        return
      }
      setSession(next)
      setUser(null)
      setPage('home')
      setGroups([])
      setFolders([])
      setRefreshVersion((version) => version + 1)
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [notify, user?.id])

  useEffect(() => {
    if (page !== 'calendar') setCalendarFocus(false)
  }, [page])

  useEffect(() => {
    if (!session) {
      setLoadingUser(false)
      setUser(null)
      return
    }

    let active = true
    setLoadingUser(true)
    api.getMe()
      .then((currentUser) => {
        if (active) setUser(currentUser)
      })
      .catch((error: unknown) => {
        if (active && readSession()) notify(error instanceof Error ? error.message : 'Could not load your profile.', 'error')
      })
      .finally(() => { if (active) setLoadingUser(false) })

    return () => { active = false }
  }, [session?.userId, notify])

  function authenticated(tokens: Parameters<typeof saveSession>[0]) {
    setSession(saveSession(tokens))
    setPage('home')
  }

  function passwordChanged() {
    clearSession()
    setSession(null)
    setUser(null)
    setPage('home')
    setGroups([])
    setFolders([])
    notify('Password updated. Please sign in again.', 'info')
  }

  async function signOut() {
    let signedOutRemotely = false
    try {
      await api.logout()
      signedOutRemotely = true
    } catch (error) {
      notify(error instanceof Error ? `Signed out locally: ${error.message}` : 'Signed out locally.', 'info')
    } finally {
      clearSession()
      setSession(null)
      setUser(null)
      setPage('home')
    }
    if (signedOutRemotely) notify('You have been signed out.', 'info')
  }

  async function saveEvent(payload: EventPayload) {
    if (eventDialog?.event) {
      await api.updateEvent(eventDialog.event.id, {
        name: payload.name,
        description: payload.description,
        startTime: payload.startTime,
        endTime: payload.endTime,
        allDay: payload.allDay,
        recurrenceUnit: payload.recurrenceUnit,
        recurrenceInterval: payload.recurrenceInterval,
        recurrenceEnd: payload.recurrenceEnd,
      })
      notify('Event updated.')
    } else {
      await api.createEvent(payload)
      notify('Event added to your calendar.')
    }
    setEventDialog(null)
    setRefreshVersion((version) => version + 1)
  }

  async function deleteEvent() {
    if (!eventDialog?.event) return
    await api.deleteEvent(eventDialog.event.id)
    setEventDialog(null)
    setRefreshVersion((version) => version + 1)
    notify('Event deleted.', 'info')
  }

  async function saveTodo(payload: TodoPayload) {
    if (todoDialog?.todo) {
      const updatingOwnTodo = todoDialog.todo.userId === user?.id
      await api.updateTodo(todoDialog.todo.id, {
        name: payload.name,
        description: payload.description,
        completed: payload.completed,
        ...(updatingOwnTodo ? { folderId: payload.folderId ?? null } : {}),
      })
      notify('Task updated.')
    } else {
      await api.createTodo(payload)
      notify('Task added to your list.')
    }
    setTodoDialog(null)
    setRefreshVersion((version) => version + 1)
  }

  async function deleteTodo() {
    if (!todoDialog?.todo) return
    await api.deleteTodo(todoDialog.todo.id)
    setTodoDialog(null)
    setRefreshVersion((version) => version + 1)
    notify('Task deleted.', 'info')
  }

  async function deleteUser(id: string) {
    await api.adminDeleteUser(id)
    setRefreshVersion((version) => version + 1)
  }

  if (!session) return <><LoginPage onAuthenticated={authenticated} /><Toasts notices={notices} onDismiss={(id) => setNotices((items) => items.filter((item) => item.id !== id))} /></>

  if (loadingUser || !user) {
    return <div className="app-loading"><span className="brand-mark"><CalendarDays size={20} /></span><span className="loading-spinner" /><p>Getting your dashboard ready…</p></div>
  }

  if (isDisplayRoute) {
    return <Suspense fallback={<div className="app-loading"><span className="brand-mark"><CalendarDays size={20} /></span><span className="loading-spinner" /><p>Opening display…</p></div>}><DisplayPage user={user} /></Suspense>
  }

  const displayPage = page === 'admin' && user.role !== 'ADMIN' ? 'home' : page
  const pageTitles: Record<Page, string> = {
    home: 'Overview',
    calendar: 'Calendar',
    todos: 'To-do list',
    groups: 'Groups',
    profile: 'Your profile',
    admin: 'Administration',
  }

  const navigation = [
    { key: 'home' as const, label: 'Overview', icon: LayoutDashboard },
    { key: 'calendar' as const, label: 'Calendar', icon: CalendarDays },
    { key: 'todos' as const, label: 'To-do list', icon: CheckSquare },
    { key: 'groups' as const, label: 'Groups', icon: Users },
  ]

  function navigate(next: Page) {
    setPage(next)
    setMobileMenuOpen(false)
    setCalendarFocus(false)
  }

  function toggleSidebar() {
    setSidebarCollapsed((collapsed) => {
      const next = !collapsed
      try {
        localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(next))
      } catch {
        // Keep the in-memory state when storage is unavailable.
      }
      return next
    })
  }

  const createButton = displayPage === 'groups'
    ? null
    : displayPage === 'todos'
      ? <button className="button button-primary topbar-create" onClick={() => setTodoDialog({})}><Plus size={16} /><span>New task</span></button>
      : <button className="button button-primary topbar-create" onClick={() => setEventDialog({ start: new Date() })}><Plus size={16} /><span>New event</span></button>

  return (
    <div className={`app-shell${sidebarCollapsed ? ' sidebar-collapsed' : ''}${calendarFocus ? ' calendar-focus' : ''}`}>
      {mobileMenuOpen && <button className="mobile-scrim" onClick={() => setMobileMenuOpen(false)} aria-label="Close navigation" />}
      <aside className={`sidebar${mobileMenuOpen ? ' sidebar-open' : ''}`}>
        <div className="sidebar-brand">
          <span className="brand-mark"><CalendarDays size={19} strokeWidth={2.2} /></span>
          <span className="brand-name">woodashboard<span>.</span></span>
          <button className="icon-button sidebar-close" onClick={() => setMobileMenuOpen(false)} aria-label="Close menu"><X size={18} /></button>
        </div>

        <div className="sidebar-label">YOUR SPACE</div>
        <nav className="sidebar-nav" aria-label="Main navigation">
          {navigation.map(({ key, label, icon: Icon }) => (
            <button className={`nav-item${displayPage === key ? ' nav-item-active' : ''}`} key={key} onClick={() => navigate(key)} title={label}>
              <Icon size={18} strokeWidth={1.8} /><span>{label}</span>{displayPage === key && <span className="nav-active-dot" />}
            </button>
          ))}
        </nav>

        {user.role === 'ADMIN' && <>
          <div className="sidebar-label sidebar-label-spaced">WORKSPACE</div>
          <nav className="sidebar-nav" aria-label="Administration">
            <button className={`nav-item${displayPage === 'admin' ? ' nav-item-active' : ''}`} onClick={() => navigate('admin')} title="Administration">
              <ShieldCheck size={18} strokeWidth={1.8} /><span>Administration</span>{displayPage === 'admin' && <span className="nav-active-dot" />}
            </button>
          </nav>
        </>}

        <div className="sidebar-bottom">
          <div className="sidebar-quote"><Sparkles size={15} /><p>Good things take<br />one day at a time.</p><span>✳</span></div>
          <button className="nav-item display-nav" onClick={() => window.open('/display', '_blank', 'noopener')} title="Open display mode">
            <Monitor size={18} strokeWidth={1.8} /><span>Display mode</span>
          </button>
          <button className={`nav-item profile-nav${displayPage === 'profile' ? ' nav-item-active' : ''}`} onClick={() => navigate('profile')} title="My profile">
            <CircleUserRound size={18} strokeWidth={1.8} /><span>My profile</span>
          </button>
          <div className="sidebar-user">
            <span className="user-avatar">{user.login.slice(0, 1).toUpperCase()}</span>
            <span className="sidebar-user-copy"><strong>{user.login}</strong><small>{user.role === 'ADMIN' ? 'Administrator' : 'Personal account'}</small></span>
            <button className="sidebar-logout" onClick={() => void signOut()} aria-label="Sign out" title="Sign out"><LogOut size={16} /></button>
          </div>
        </div>
      </aside>

      <main className="main-shell">
        <header className="topbar">
          <button className="icon-button desktop-sidebar-toggle" onClick={toggleSidebar} aria-label={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'} title={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}>
            {sidebarCollapsed ? <PanelLeftOpen size={19} /> : <PanelLeftClose size={19} />}
          </button>
          <button className="icon-button mobile-menu-button" onClick={() => setMobileMenuOpen(true)} aria-label="Open menu"><Menu size={20} /></button>
          <div className="topbar-title"><span className="eyebrow">HOME DASHBOARD</span><strong>{pageTitles[displayPage]}</strong></div>
          <div className="topbar-right">
            <span className="topbar-date">{new Intl.DateTimeFormat(undefined, { weekday: 'short', month: 'short', day: 'numeric' }).format(new Date())}</span>
            {createButton}
            <button className="topbar-avatar" onClick={() => navigate('profile')} aria-label="Open profile">{user.login.slice(0, 1).toUpperCase()}</button>
          </div>
        </header>

        {displayPage === 'home' && <DashboardPage user={user} refreshVersion={refreshVersion} onNavigate={navigate} onCreateEvent={() => setEventDialog({ start: new Date() })} onEditEvent={(event) => setEventDialog({ event })} onCreateTodo={() => setTodoDialog({})} onEditTodo={(todo) => setTodoDialog({ todo })} onNotice={notify} />}
        {displayPage === 'calendar' && <Suspense fallback={<div className="admin-loading page-loading"><span className="loading-spinner" /> Opening your calendar…</div>}><CalendarPage groups={groups} refreshVersion={refreshVersion} focusMode={calendarFocus} onToggleFocus={() => setCalendarFocus((focused) => !focused)} onCreateEvent={(start, allDay) => setEventDialog({ start, allDay })} onEditEvent={(event) => setEventDialog({ event })} onNotice={notify} /></Suspense>}
        {displayPage === 'todos' && <TodosPage groups={groups} folders={folders} refreshVersion={refreshVersion} onCreateTodo={() => setTodoDialog({})} onEditTodo={(todo) => setTodoDialog({ todo })} onFoldersChanged={async () => { await loadFolders(); setRefreshVersion((version) => version + 1) }} onNotice={notify} />}
        {displayPage === 'groups' && <GroupsPage groups={groups} currentUser={user} onGroupsChanged={async () => { await loadGroups(); setRefreshVersion((version) => version + 1) }} onNotice={notify} />}
        {displayPage === 'profile' && <ProfilePage user={user} session={session} onSignOut={signOut} onPasswordChanged={passwordChanged} onNotice={notify} />}
        {displayPage === 'admin' && <AdminPage currentUser={user} refreshVersion={refreshVersion} onDeleteUser={deleteUser} onNotice={notify} />}
      </main>

      {eventDialog && <EventFormModal event={eventDialog.event} initialStart={eventDialog.start} initialAllDay={eventDialog.allDay} groups={groups} onClose={() => setEventDialog(null)} onSave={saveEvent} onDelete={eventDialog.event ? deleteEvent : undefined} />}
      {todoDialog && <TodoFormModal todo={todoDialog.todo} groups={groups} folders={folders} currentUserId={user.id} onClose={() => setTodoDialog(null)} onSave={saveTodo} onDelete={todoDialog.todo ? deleteTodo : undefined} />}
      <Toasts notices={notices} onDismiss={(id) => setNotices((items) => items.filter((item) => item.id !== id))} />
    </div>
  )
}
