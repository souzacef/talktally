import { describe, expect, it } from 'vitest'
import { transactionCountText } from '@/features/transactions/transaction-messages'

describe('transaction count messages', () => {
  it.each([
    ['en-US', 'records', 0, '0 records'],
    ['en-US', 'records', 1, '1 record'],
    ['en-US', 'records', 2, '2 records'],
    ['pt-BR', 'records', 0, '0 registros'],
    ['pt-BR', 'records', 1, '1 registro'],
    ['pt-BR', 'records', 2, '2 registros'],
    ['en-US', 'installments', 0, '0 installments'],
    ['en-US', 'installments', 1, '1 installment'],
    ['en-US', 'installments', 2, '2 installments'],
    ['pt-BR', 'installments', 0, '0 parcelas'],
    ['pt-BR', 'installments', 1, '1 parcela'],
    ['pt-BR', 'installments', 2, '2 parcelas'],
  ] as const)(
    'pluralizes %s in %s for %i',
    (locale, key, count, expected) => {
      expect(transactionCountText(locale, key, count)).toBe(expected)
    },
  )
})
