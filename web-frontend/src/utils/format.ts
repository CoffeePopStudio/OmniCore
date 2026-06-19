export function formatTimestamp(raw: string): string {
  if (!raw) return '—'
  try {
    const date = new Date(raw)
    if (isNaN(date.getTime())) return raw
    return new Intl.DateTimeFormat(undefined, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date)
  } catch {
    return raw
  }
}

export function safeCount(value: number | undefined | null): string {
  if (value === undefined || value === null || value === 0) return '—'
  return value.toLocaleString()
}
