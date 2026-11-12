import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import { CalendarDays, CheckSquare, CloudSun, LoaderCircle } from 'lucide-react'
import { api } from '../api'
import { allDayEndExclusive, formatDate, formatLocalDateTime } from '../format'
import { useAutoRefresh } from '../hooks/useAutoRefresh'
import type { EventItem, TodoItem, User } from '../types'

interface DisplayPageProps {
  user: User
}

interface DateRange {
  start: Date
  end: Date
}

const MAX_TODOS = 20

export function DisplayPage({ user }: DisplayPageProps) {
  const [events, setEvents] = useState<EventItem[]>([])
  const [todos, setTodos] = useState<TodoItem[]>([])
  const [todoCount, setTodoCount] = useState(0)
  const [loadingTodos, setLoadingTodos] = useState(true)
  const [error, setError] = useState('')
  const [now, setNow] = useState(() => new Date())
  const rangeRef = useRef<DateRange | null>(null)

  const loadEvents = useCallback(async () => {
    const range = rangeRef.current
    if (!range) return
    try {
      const inclusiveEnd = new Date(range.end.getTime() - 1000)
      const result = await api.listEvents(formatLocalDateTime(range.start), formatLocalDateTime(inclusiveEnd))
      setEvents(result.sort((a, b) => a.startTime.localeCompare(b.startTime)))
      setError('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not load calendar events.')
    }
  }, [])

  const loadTodos = useCallback(async (background = false) => {
    if (!background) setLoadingTodos(true)
    try {
      const result = await api.listAccessibleTodos(false)
      setTodoCount(result.length)
      setTodos(result.sort((a, b) => a.creationTime.localeCompare(b.creationTime)).slice(0, MAX_TODOS))
      setError('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not load open tasks.')
    } finally {
      if (!background) setLoadingTodos(false)
    }
  }, [])

  useEffect(() => {
    void loadTodos()
  }, [loadTodos])

  useAutoRefresh(() => {
    void loadEvents()
    void loadTodos(true)
  })

  useEffect(() => {
    const interval = window.setInterval(() => setNow(new Date()), 30_000)
    return () => window.clearInterval(interval)
  }, [])

  const calendarEvents = useMemo(() => events.map((event, index) => ({
    id: `${event.id}@${event.startTime}`,
    title: event.name,
    start: event.allDay ? event.startTime.slice(0, 10) : event.startTime,
    end: event.allDay ? allDayEndExclusive(event.endTime) : event.endTime,
    allDay: event.allDay,
    extendedProps: { event },
    classNames: [`calendar-event-tone-${index % 3}`],
  })), [events])

  return (
    <div className="display-shell">
      <header className="display-header">
        <div className="display-brand">
          <span className="brand-mark"><CalendarDays size={22} /></span>
          <div>
            <span className="eyebrow">WOODASHBOARD DISPLAY</span>
            <strong>{formatDate(now)}</strong>
          </div>
        </div>
        <div className="display-clock">{new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(now)}</div>
        <div className="display-user"><CloudSun size={15} /> {user.login}</div>
      </header>

      {error && <div className="display-error" role="alert">{error}</div>}

      <div className="display-grid">
        <section className="display-card display-calendar-card">
          <div className="display-card-heading">
            <div>
              <span className="eyebrow">THIS MONTH</span>
              <h2>Calendar</h2>
            </div>
            <span className="display-card-icon"><CalendarDays size={18} /></span>
          </div>
          <div className="display-calendar-wrap">
            <FullCalendar
              plugins={[dayGridPlugin]}
              initialView="dayGridMonth"
              headerToolbar={{ left: 'prev,next today', center: 'title', right: '' }}
              buttonText={{ today: 'Today' }}
              events={calendarEvents}
              datesSet={(info) => {
                rangeRef.current = { start: info.start, end: info.end }
                void loadEvents()
              }}
              height="100%"
              expandRows
              dayMaxEvents={6}
              firstDay={1}
              eventTimeFormat={{ hour: 'numeric', minute: '2-digit', meridiem: 'short' }}
              dayHeaderFormat={{ weekday: 'short' }}
              noEventsContent="No plans this month."
            />
          </div>
        </section>

        <section className="display-card display-todos-card">
          <div className="display-card-heading">
            <div>
              <span className="eyebrow">OPEN TASKS</span>
              <h2>To-do list</h2>
            </div>
            <span className="display-card-icon"><CheckSquare size={18} /></span>
          </div>
          {loadingTodos && todos.length === 0 ? (
            <div className="display-loading"><LoaderCircle className="spin" size={20} /> Loading tasks…</div>
          ) : todos.length === 0 ? (
            <div className="display-empty"><CheckSquare size={26} /><strong>All clear</strong><span>No open tasks.</span></div>
          ) : (
            <div className="display-todos">
              {todos.map((todo) => (
                <div className="display-todo" key={todo.id}>
                  <span className={`display-todo-dot ${todo.groupId ? 'display-todo-shared' : ''}`} />
                  <div>
                    <strong>{todo.name}</strong>
                    <small>
                      {todo.groupId ? `Shared · ${todo.userLogin}` : 'Personal'}
                      {todo.description ? ` · ${todo.description}` : ''}
                    </small>
                  </div>
                </div>
              ))}
            </div>
          )}
          <div className="display-footer-note">
            {todoCount} open task{todoCount === 1 ? '' : 's'}
            {todoCount > MAX_TODOS ? ` · showing ${MAX_TODOS}` : ''} · auto-refresh every 60s
          </div>
        </section>
      </div>
    </div>
  )
}
