import { lazy, Suspense, useCallback, useEffect, useState } from 'react'
import {
  CalendarDays,
  CheckSquare,
  CircleUserRound,
  LayoutDashboard,
  LogOut,
  Menu,
  Plus,
  ShieldCheck,
  Sparkles,
  X,
} from 'lucide-react'
import { api, SESSION_EXPIRED_EVENT } from './api'
import { EventFormModal } from './components/EventFormModal'
import { LoginPage } from './components/LoginPage'
import { TodoFormModal } from './components/TodoFormModal'
import { Toasts } from './components/Toasts'
import { clearSession, readSession, saveSession } from './session'
import { AdminPage } from './pages/AdminPage'
import { DashboardPage } from './pages/DashboardPage'
import { ProfilePage } from './pages/ProfilePage'
import { TodosPage } from './pages/TodosPage'
import type { EventItem, EventPayload, Page, Session, TodoItem, TodoPayload, ToastNotice, User } from './types'

const CalendarPage = lazy(() => import('./pages/CalendarPage').then((module) => ({ default: module.CalendarPage })))

type EventDialog = { event?: EventItem; start?: Date } | null
type TodoDialog = { todo?: TodoItem } | null

export default function App() {
  const [session, setSession] = useState<Session | null>(() => readSession())
  const [user, setUser] = useState<User | null>(null)
  const [loadingUser, setLoadingUser] = useState(Boolean(readSession()))
  const [page, setPage] = useState<Page>('home')
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [eventDialog, setEventDialog] = useState<EventDialog>(null)
  const [todoDialog, setTodoDialog] = useState<TodoDialog>(null)
  const [refreshVersion, setRefreshVersion] = useState(0)
  const [notices, setNotices] = useState<ToastNotice[]>([])

  const notify = useCallback((message: string, kind: ToastNotice['kind'] = 'success') => {
    const id = Date.now() + Math.random()
    setNotices((items) => [...items, { id, message, kind }].slice(-4))
    window.setTimeout(() => setNotices((items) => items.filter((item) => item.id !== id)), 4500)
  }, [])

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
  }, [session?.accessToken, notify])

  function authenticated(tokens: Parameters<typeof saveSession>[0]) {
    setSession(saveSession(tokens))
    setPage('home')
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
      await api.updateEvent(eventDialog.event.id, payload)
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
      await api.updateTodo(todoDialog.todo.id, payload)
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

  const displayPage = page === 'admin' && user.role !== 'ADMIN' ? 'home' : page
  const pageTitles: Record<Page, string> = {
    home: 'Overview',
    calendar: 'Calendar',
    todos: 'To-do list',
    profile: 'Your profile',
    admin: 'Administration',
  }

  const navigation = [
    { key: 'home' as const, label: 'Overview', icon: LayoutDashboard },
    { key: 'calendar' as const, label: 'Calendar', icon: CalendarDays },
    { key: 'todos' as const, label: 'To-do list', icon: CheckSquare },
  ]

  function navigate(next: Page) {
    setPage(next)
    setMobileMenuOpen(false)
  }

  const createButton = displayPage === 'todos'
    ? <button className="button button-primary topbar-create" onClick={() => setTodoDialog({})}><Plus size={16} /><span>New task</span></button>
    : <button className="button button-primary topbar-create" onClick={() => setEventDialog({ start: new Date() })}><Plus size={16} /><span>New event</span></button>

  return (
    <div className="app-shell">
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
            <button className={`nav-item${displayPage === key ? ' nav-item-active' : ''}`} key={key} onClick={() => navigate(key)}>
              <Icon size={18} strokeWidth={1.8} /><span>{label}</span>{displayPage === key && <span className="nav-active-dot" />}
            </button>
          ))}
        </nav>

        {user.role === 'ADMIN' && <>
          <div className="sidebar-label sidebar-label-spaced">WORKSPACE</div>
          <nav className="sidebar-nav" aria-label="Administration">
            <button className={`nav-item${displayPage === 'admin' ? ' nav-item-active' : ''}`} onClick={() => navigate('admin')}>
              <ShieldCheck size={18} strokeWidth={1.8} /><span>Administration</span>{displayPage === 'admin' && <span className="nav-active-dot" />}
            </button>
          </nav>
        </>}

        <div className="sidebar-bottom">
          <div className="sidebar-quote"><Sparkles size={15} /><p>Good things take<br />one day at a time.</p><span>✳</span></div>
          <button className={`nav-item profile-nav${displayPage === 'profile' ? ' nav-item-active' : ''}`} onClick={() => navigate('profile')}>
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
          <button className="icon-button mobile-menu-button" onClick={() => setMobileMenuOpen(true)} aria-label="Open menu"><Menu size={20} /></button>
          <div className="topbar-title"><span className="eyebrow">HOME DASHBOARD</span><strong>{pageTitles[displayPage]}</strong></div>
          <div className="topbar-right">
            <span className="topbar-date">{new Intl.DateTimeFormat(undefined, { weekday: 'short', month: 'short', day: 'numeric' }).format(new Date())}</span>
            {createButton}
            <button className="topbar-avatar" onClick={() => navigate('profile')} aria-label="Open profile">{user.login.slice(0, 1).toUpperCase()}</button>
          </div>
        </header>

        {displayPage === 'home' && <DashboardPage user={user} refreshVersion={refreshVersion} onNavigate={navigate} onCreateEvent={() => setEventDialog({ start: new Date() })} onEditEvent={(event) => setEventDialog({ event })} onCreateTodo={() => setTodoDialog({})} onEditTodo={(todo) => setTodoDialog({ todo })} onNotice={notify} />}
        {displayPage === 'calendar' && <Suspense fallback={<div className="admin-loading page-loading"><span className="loading-spinner" /> Opening your calendar…</div>}><CalendarPage refreshVersion={refreshVersion} onCreateEvent={(start) => setEventDialog({ start })} onEditEvent={(event) => setEventDialog({ event })} onNotice={notify} /></Suspense>}
        {displayPage === 'todos' && <TodosPage user={user} refreshVersion={refreshVersion} onCreateTodo={() => setTodoDialog({})} onEditTodo={(todo) => setTodoDialog({ todo })} onNotice={notify} />}
        {displayPage === 'profile' && <ProfilePage user={user} session={session} onSignOut={signOut} onNotice={notify} />}
        {displayPage === 'admin' && <AdminPage currentUser={user} refreshVersion={refreshVersion} onDeleteUser={deleteUser} onNotice={notify} />}
      </main>

      {eventDialog && <EventFormModal event={eventDialog.event} initialStart={eventDialog.start} onClose={() => setEventDialog(null)} onSave={saveEvent} onDelete={eventDialog.event ? deleteEvent : undefined} />}
      {todoDialog && <TodoFormModal todo={todoDialog.todo} onClose={() => setTodoDialog(null)} onSave={saveTodo} onDelete={todoDialog.todo ? deleteTodo : undefined} />}
      <Toasts notices={notices} onDismiss={(id) => setNotices((items) => items.filter((item) => item.id !== id))} />
    </div>
  )
}
