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
