import type { DecimalValue } from '@/types/api'

const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function formatBrl(value: DecimalValue): string {
  const numericValue = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(numericValue)) {
    throw new Error('money value is not a valid decimal')
  }
  return brlFormatter.format(numericValue)
}
