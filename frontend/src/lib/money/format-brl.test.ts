import { describe, expect, it } from 'vitest'
import { formatBrl } from '@/lib/money/format-brl'

describe('formatBrl', () => {
  it('formats decimal strings for pt-BR display', () => {
    expect(formatBrl('1234.56')).toMatch(/^R\$\s*1\.234,56$/)
  })

  it('rejects non-decimal display values', () => {
    expect(() => formatBrl('not-money')).toThrow(/valid decimal/)
  })
})
