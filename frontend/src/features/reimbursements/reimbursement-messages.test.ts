import { describe, expect, it } from 'vitest'
import { reimbursementCountText } from '@/features/reimbursements/reimbursement-messages'

describe('reimbursement count messages', () => {
  it.each([
    ['en-US', 0, '0 installments'],
    ['en-US', 1, '1 installment'],
    ['en-US', 2, '2 installments'],
    ['pt-BR', 0, '0 parcelas'],
    ['pt-BR', 1, '1 parcela'],
    ['pt-BR', 2, '2 parcelas'],
  ] as const)('pluralizes installments in %s for %i', (locale, count, expected) => {
    expect(reimbursementCountText(locale, 'installments', count)).toBe(expected)
  })
})
