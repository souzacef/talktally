import type { TransactionSource } from '@/types/api'

const sourceLabels: Record<TransactionSource, string> = {
  MANUAL: 'Manual',
  ASSISTANT_TEXT: 'Assistant',
  VOICE: 'Voice',
}

const eventDateFormatter = new Intl.DateTimeFormat('en', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
})

export function transactionSourceLabel(source: TransactionSource): string {
  return sourceLabels[source]
}

export function formatEventDate(date: string): string {
  return eventDateFormatter.format(new Date(`${date}T00:00:00Z`))
}
