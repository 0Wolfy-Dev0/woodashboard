import { useCallback, useEffect, useState } from 'react'
import {
  ArrowRight,
  CalendarClock,
  CalendarPlus,
  Check,
  CheckCircle2,
  Circle,
  Clock3,
  ListTodo,
  Plus,
  Sparkles,
} from 'lucide-react'
import { api } from '../api'
import { formatDate, formatLocalDateTime, formatTime, relativeDayLabel } from '../format'
import { useAutoRefresh } from '../hooks/useAutoRefresh'
import type { EventItem, Page, TodoItem, ToastNotice, User } from '../types'

interface DashboardPageProps {
  user: User
  refreshVersion: number
  onNavigate: (page: Page) => void
  onCreateEvent: () => void
  onEditEvent: (event: EventItem) => void
  onCreateTodo: () => void
  onEditTodo: (todo: TodoItem) => void
  onNotice: (message: string, kind?: ToastNotice['kind']) => void
}

export function DashboardPage({
  user,
  refreshVersion,
  onNavigate,
  onCreateEvent,
  onEditEvent,
  onCreateTodo,
  onEditTodo,
  onNotice,
}: DashboardPageProps) {
  const [events, setEvents] = useState<EventItem[]>([])
  const [openTodos, setOpenTodos] = useState<TodoItem[]>([])
  const [completedCount, setCompletedCount] = useState(0)
  const [loading, setLoading] = useState(true)

  const loadDashboard = useCallback(async (background = false) => {
    const now = new Date()
    const startOfToday = new Date(now)
    startOfToday.setHours(0, 0, 0, 0)
    const nextWeek = new Date(now)
    nextWeek.setDate(nextWeek.getDate() + 7)

    try {
      const [upcomingEvents, todos, completed] = await Promise.all([
        api.listEvents(formatLocalDateTime(startOfToday), formatLocalDateTime(nextWeek)),
        api.listTodos(user.id, false),
        api.listTodos(user.id, true),
      ])
      setEvents(upcomingEvents.sort((a, b) => a.startTime.localeCompare(b.startTime)))
      setOpenTodos(todos)
      setCompletedCount(completed.length)
    } catch (error) {
      if (!background) onNotice(error instanceof Error ? error.message : 'Could not load your dashboard.', 'error')
    } finally {
      setLoading(false)
    }
  }, [user.id, onNotice])

  useEffect(() => {
    void loadDashboard()
  }, [loadDashboard, refreshVersion])

  useAutoRefresh(() => loadDashboard(true))

  const hour = new Date().getHours()
  const salutation = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening'
  const todayKey = new Date().toDateString()
  const todayEvents = events.filter((event) => new Date(event.startTime).toDateString() === todayKey)
  const upcomingEvents = events.filter((event) => new Date(event.endTime).getTime() >= Date.now())
  const nextEvent = upcomingEvents[0]

  async function toggleTodo(todo: TodoItem) {
    try {
      await api.updateTodo(todo.id, { completed: true })
      setOpenTodos((items) => items.filter((item) => item.id !== todo.id))
      setCompletedCount((count) => count + 1)
      onNotice('Task marked complete. Nice work.')
    } catch (error) {
      onNotice(error instanceof Error ? error.message : 'Could not update this task.', 'error')
    }
  }

  return (
    <div className="page-content dashboard-page">
      <section className="welcome-panel">
        <div className="welcome-copy">
          <span className="eyebrow eyebrow-light"><Sparkles size={14} /> YOUR SPACE, YOUR PACE</span>
          <h1>{salutation}, {user.login}<span className="welcome-period">.</span></h1>
          <p>A little overview to help you find your flow today.</p>
          <div className="welcome-actions">
            <button className="button button-light" onClick={onCreateEvent}><CalendarPlus size={16} /> New event</button>
            <button className="button button-glass" onClick={onCreateTodo}><Plus size={16} /> Add a task</button>
          </div>
        </div>
        <div className="welcome-date-card">
          <span className="date-card-icon"><CalendarClock size={20} /></span>
          <span className="date-card-label">TODAY</span>
          <strong>{formatDate(new Date())}</strong>
          <span className="date-card-time">A fresh start, one thing at a time.</span>
          <span className="date-card-watermark">{new Date().getDate()}</span>
        </div>
        <span className="welcome-orbit welcome-orbit-one" />
        <span className="welcome-orbit welcome-orbit-two" />
      </section>

      <div className="stat-grid">
        <article className="stat-card stat-card-sand">
          <span className="stat-icon stat-icon-sand"><ListTodo size={18} /></span>
          <span className="stat-label">OPEN TASKS</span>
          <strong>{loading ? '—' : openTodos.length}</strong>
          <span className="stat-foot">Ready when you are</span>
        </article>
        <article className="stat-card stat-card-green">
          <span className="stat-icon stat-icon-green"><CalendarClock size={18} /></span>
          <span className="stat-label">TODAY’S PLANS</span>
          <strong>{loading ? '—' : todayEvents.length}</strong>
          <span className="stat-foot">On your calendar</span>
        </article>
        <article className="stat-card stat-card-lilac">
          <span className="stat-icon stat-icon-lilac"><CheckCircle2 size={18} /></span>
          <span className="stat-label">TASKS COMPLETED</span>
          <strong>{loading ? '—' : completedCount}</strong>
          <span className="stat-foot">You’ve got this</span>
        </article>
      </div>

      <div className="dashboard-grid">
        <section className="surface-card agenda-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">A LOOK AHEAD</span>
              <h2>Coming up</h2>
            </div>
            <button className="text-button" onClick={() => onNavigate('calendar')}>Open calendar <ArrowRight size={15} /></button>
          </div>
          {loading ? (
            <div className="loading-list"><span /><span /><span /></div>
          ) : upcomingEvents.length === 0 ? (
            <div className="empty-state compact-empty">
              <span className="empty-icon"><CalendarClock size={20} /></span>
              <strong>Nothing on the calendar yet</strong>
              <p>Give future-you something to look forward to.</p>
              <button className="button button-secondary button-small" onClick={onCreateEvent}><Plus size={15} /> Add an event</button>
            </div>
          ) : (
            <div className="agenda-list">
              {upcomingEvents.slice(0, 5).map((event, index) => (
                <button className="agenda-row" key={event.id} onClick={() => onEditEvent(event)}>
                  <span className={`agenda-marker agenda-marker-${index % 3}`} />
                  <span className="agenda-time-block"><strong>{relativeDayLabel(event.startTime)}</strong><small>{formatTime(event.startTime)}</small></span>
                  <span className="agenda-event-copy"><strong>{event.name}</strong><small>{event.description || 'Calendar event'}</small></span>
                  <ArrowRight className="agenda-arrow" size={16} />
                </button>
              ))}
            </div>
          )}
          {nextEvent && <p className="agenda-footnote"><Clock3 size={13} /> Showing the next 7 days, based on your calendar.</p>}
        </section>

        <section className="surface-card task-preview-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">SMALL STEPS ADD UP</span>
              <h2>Your task list</h2>
            </div>
            <button className="icon-button icon-button-soft" onClick={onCreateTodo} aria-label="Add task"><Plus size={18} /></button>
          </div>
          {loading ? (
            <div className="loading-list"><span /><span /><span /></div>
          ) : openTodos.length === 0 ? (
            <div className="empty-state compact-empty">
              <span className="empty-icon"><CheckCircle2 size={20} /></span>
              <strong>All caught up</strong>
              <p>Enjoy the breathing room, or add a new task.</p>
              <button className="button button-secondary button-small" onClick={onCreateTodo}><Plus size={15} /> Add a task</button>
            </div>
          ) : (
            <div className="task-preview-list">
              {openTodos.slice(0, 5).map((todo) => (
                <div className="task-preview-row" key={todo.id}>
                  <button className="task-check" onClick={() => toggleTodo(todo)} aria-label={`Mark ${todo.name} complete`}><Circle size={19} /></button>
                  <button className="task-preview-copy" onClick={() => onEditTodo(todo)}>
                    <strong>{todo.name}</strong>
                    {todo.description && <small>{todo.description}</small>}
                  </button>
                  <button className="task-edit-dot" onClick={() => onEditTodo(todo)} aria-label={`Edit ${todo.name}`}><ArrowRight size={15} /></button>
                </div>
              ))}
            </div>
          )}
          <button className="button button-secondary task-view-all" onClick={() => onNavigate('todos')}>
            <Check size={15} /> View all tasks <ArrowRight size={15} />
          </button>
        </section>
      </div>

      <section className="gentle-reminder">
        <span className="gentle-reminder-icon"><Sparkles size={17} /></span>
        <div><strong>A gentle reminder</strong><p>Not everything needs to be done today. Choose what matters most.</p></div>
        <span className="gentle-reminder-decoration">✳</span>
      </section>
    </div>
  )
}
