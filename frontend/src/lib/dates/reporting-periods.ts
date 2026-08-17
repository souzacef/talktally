export interface DateRange {
  from: string
  to: string
}

function isoDate(year: number, monthIndex: number, day: number): string {
  return `${year}-${String(monthIndex + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

export function currentMonthRange(now = new Date()): DateRange {
  const year = now.getFullYear()
  const month = now.getMonth()
  return {
    from: isoDate(year, month, 1),
    to: isoDate(year, month, new Date(year, month + 1, 0).getDate()),
  }
}

export function trailingSixMonthRange(now = new Date()): DateRange {
  const year = now.getFullYear()
  const month = now.getMonth()
  const firstMonth = new Date(year, month - 5, 1)
  return {
    from: isoDate(firstMonth.getFullYear(), firstMonth.getMonth(), 1),
    to: isoDate(year, month, new Date(year, month + 1, 0).getDate()),
  }
}
