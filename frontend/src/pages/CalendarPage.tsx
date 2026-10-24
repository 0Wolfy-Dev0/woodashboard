import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import listPlugin from '@fullcalendar/list'
import interactionPlugin, { type DateClickArg } from '@fullcalendar/interaction'
import { CalendarDays, CalendarPlus, Clock3, LoaderCircle, RefreshCw, Sparkles } from 'lucide-react'
import { api } from '../api'
import { allDayEndExclusive, formatDate, formatEventTime, formatLocalDateTime, relativeDayLabel } from '../format'
import { useAutoRefresh } from '../hooks/useAutoRefresh'
import type { EventItem, Group, ToastNotice } from '../types'

interface CalendarPageProps {
  groups: Group[]
  refreshVersion: number
  onCreateEvent: (start: Date, allDay: boolean) => void
  onEditEvent: (event: EventItem) => void
  onNotice: (message: string, kind?: ToastNotice['kind']) => void
}

interface DateRange {
  start: Date
  end: Date
}

export function CalendarPage({ groups, refreshVersion, onCreateEvent, onEditEvent, onNotice }: CalendarPageProps) {
  const [events, setEvents] = useState<EventItem[]>([])
  const [groupId, setGroupId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const rangeRef = useRef<DateRange | null>(null)

  const loadEvents = useCallback(async (background = false) => {
    const range = rangeRef.current
    if (!range) return
    if (!background) setLoading(true)
    setError('')
    try {
      // FullCalendar's visible range end is exclusive; the API's `to` is inclusive.
      const inclusiveEnd = new Date(range.end.getTime() - 1000)
      const result = await api.listEvents(formatLocalDateTime(range.start), formatLocalDateTime(inclusiveEnd),
        groupId || undefined)
      setEvents(result.sort((a, b) => a.startTime.localeCompare(b.startTime)))
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Could not load calendar events.'
      setError(message)
      if (!background) onNotice(message, 'error')
    } finally {
      if (!background) setLoading(false)
    }
  }, [groupId, onNotice])

  useEffect(() => {
    void loadEvents()
  }, [loadEvents, refreshVersion])

  useEffect(() => {
    if (groupId && !groups.some((group) => group.id === groupId)) setGroupId('')
  }, [groupId, groups])

  useAutoRefresh(() => loadEvents(true))

  const calendarEvents = useMemo(() => events.map((event, index) => ({
    id: `${event.id}@${event.startTime}`,
    title: event.name,
    start: event.allDay ? event.startTime.slice(0, 10) : event.startTime,
    end: event.allDay ? allDayEndExclusive(event.endTime) : event.endTime,
    allDay: event.allDay,
    extendedProps: { event },
    classNames: [`calendar-event-tone-${index % 3}`],
  })), [events])

  const upcoming = useMemo(() => {
    const now = Date.now()
    return events.filter((event) => new Date(event.endTime).getTime() >= now).slice(0, 4)
  }, [events])

  function createAtDate(info: DateClickArg) {
    onCreateEvent(new Date(info.date), info.allDay)
  }

  return (
    <div className="page-content calendar-page">
      <div className="page-heading calendar-page-heading">
        <div>
          <span className="eyebrow">MAKE ROOM FOR WHAT MATTERS</span>
          <h1>Your calendar</h1>
          <p>Plans, little milestones, and everything in between.</p>
        </div>
        <div className="heading-actions">
          <select className="select-control" value={groupId} onChange={(e) => setGroupId(e.target.value)} aria-label="Filter events by group">
            <option value="">All calendars</option>
            {groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
          </select>
          <button className="button button-secondary" type="button" onClick={() => void loadEvents()} disabled={loading}>
            {loading ? <LoaderCircle className="spin" size={16} /> : <RefreshCw size={15} />} Refresh
          </button>
          <button className="button button-primary" type="button" onClick={() => onCreateEvent(new Date(), false)}><CalendarPlus size={16} /> Add event</button>
        </div>
      </div>

      <div className="calendar-layout">
        <section className="surface-card calendar-surface">
          <div className="calendar-legend">
            <span className="calendar-legend-dot" /> Your events
            <span className="calendar-filter-note"><Sparkles size={13} /> Events are filtered to the visible date range.</span>
          </div>
          {error && <div className="inline-error" role="alert">{error}</div>}
          <div className="full-calendar-wrap">
            <FullCalendar
              plugins={[dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin]}
              initialView="dayGridMonth"
              headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek' }}
              buttonText={{ today: 'Today', month: 'Month', week: 'Week', day: 'Day', list: 'Agenda' }}
              events={calendarEvents}
              datesSet={(info) => {
                rangeRef.current = { start: info.start, end: info.end }
                void loadEvents()
              }}
              dateClick={createAtDate}
              eventClick={(info) => {
                const event = info.event.extendedProps.event as EventItem | undefined
                if (event) onEditEvent(event)
              }}
              height="auto"
              dayMaxEvents={3}
              nowIndicator
              firstDay={1}
              eventTimeFormat={{ hour: 'numeric', minute: '2-digit', meridiem: 'short' }}
              dayHeaderFormat={{ weekday: 'short' }}
              noEventsContent="A little breathing room. Click a date to add a plan."
            />
          </div>
        </section>

        <aside className="calendar-aside">
          <section className="surface-card calendar-aside-card">
            <div className="section-heading section-heading-small">
              <div><span className="eyebrow">UP NEXT</span><h2>Coming up</h2></div>
              <span className="aside-heading-icon"><Clock3 size={17} /></span>
            </div>
            {upcoming.length === 0 ? (
              <div className="empty-state compact-empty compact-empty-aside">
                <span className="empty-icon"><CalendarDays size={19} /></span>
                <strong>Nothing coming up</strong>
                <p>Tap a date to add your next plan.</p>
              </div>
            ) : (
              <div className="upcoming-list">
                {upcoming.map((event, index) => (
                  <button className="upcoming-event" key={event.id} onClick={() => onEditEvent(event)}>
                    <span className={`upcoming-event-bar upcoming-event-bar-${index % 3}`} />
                    <span className="upcoming-event-main"><strong>{event.name}</strong><small>{relativeDayLabel(event.startTime)} · {formatEventTime(event)}{event.recurrenceUnit ? ' · Repeats' : ''}</small></span>
                    {event.description && <span className="upcoming-event-description">{event.description}</span>}
                  </button>
                ))}
              </div>
            )}
          </section>

          <section className="calendar-tip-card">
            <span className="calendar-tip-icon"><Sparkles size={16} /></span>
            <span className="eyebrow">A SMALL TIP</span>
            <p>Click a date to add a plan, or click an event to open and edit it.</p>
          </section>

          <div className="calendar-today-note"><CalendarDays size={14} /><span>Today is {formatDate(new Date())}</span></div>
        </aside>
      </div>
    </div>
  )
}
