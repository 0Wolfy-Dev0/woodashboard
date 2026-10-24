const dateFormatter = new Intl.DateTimeFormat(undefined, {
  weekday: 'short',
  month: 'short',
  day: 'numeric',
})

const longDateFormatter = new Intl.DateTimeFormat(undefined, {
  weekday: 'long',
  month: 'long',
  day: 'numeric',
  year: 'numeric',
})

const timeFormatter = new Intl.DateTimeFormat(undefined, {
  hour: 'numeric',
  minute: '2-digit',
})

const recurrenceNames = {
  DAYS: ['day', 'days'],
  WEEKS: ['week', 'weeks'],
  MONTHS: ['month', 'months'],
  YEARS: ['year', 'years'],
} as const

export function formatLocalDate(value: Date): string {
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
}

export function formatDate(value: string | Date): string {
  return dateFormatter.format(value instanceof Date ? value : new Date(value))
}

export function formatLongDate(value: string | Date): string {
  return longDateFormatter.format(value instanceof Date ? value : new Date(value))
}

export function formatTime(value: string | Date): string {
  return timeFormatter.format(value instanceof Date ? value : new Date(value))
}

export function formatLocalDateTime(value: Date): string {
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

export function toDateInput(value: string): string {
  return value.slice(0, 10)
}

export function fromDateInput(value: string): string {
  return value.length === 10 ? `${value}T00:00:00` : value
}

/** FullCalendar treats all-day ends as exclusive, while the API stores the last inclusive day. */
export function allDayEndExclusive(value: string): string {
  const [year, month, day] = value.slice(0, 10).split('-').map(Number)
  return formatLocalDate(new Date(year, month - 1, day + 1))
}

export function formatEventTime(event: { allDay: boolean; startTime: string }): string {
  return event.allDay ? 'All day' : formatTime(event.startTime)
}

export function formatRecurrence(unit: keyof typeof recurrenceNames | null, interval: number | null): string {
  if (!unit) return 'Does not repeat'
  const count = interval ?? 1
  const [singular, plural] = recurrenceNames[unit]
  return count === 1 ? `Every ${singular}` : `Every ${count} ${plural}`
}

export function toDateTimeInput(value: string): string {
  return value.slice(0, 16)
}

export function fromDateTimeInput(value: string): string {
  return value.length === 16 ? `${value}:00` : value
}

export function dayBounds(date = new Date()): { from: string; to: string } {
  const start = new Date(date)
  start.setHours(0, 0, 0, 0)
  const end = new Date(start)
  end.setDate(end.getDate() + 1)
  end.setSeconds(end.getSeconds() - 1)
  return { from: formatLocalDateTime(start), to: formatLocalDateTime(end) }
}

export function relativeDayLabel(value: string): string {
  const date = new Date(value)
  const today = new Date()
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  const sameDate = (left: Date, right: Date) =>
    left.getFullYear() === right.getFullYear() &&
    left.getMonth() === right.getMonth() &&
    left.getDate() === right.getDate()

  if (sameDate(date, today)) return 'Today'
  if (sameDate(date, tomorrow)) return 'Tomorrow'
  return formatDate(date)
}
