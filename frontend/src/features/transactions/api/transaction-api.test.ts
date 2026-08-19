import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { TransactionRequest } from '@/types/api'

const mocks = vi.hoisted(() => ({
  post: vi.fn(),
  put: vi.fn(),
}))

vi.mock('@/lib/api/api-client', () => ({
  apiClient: {
    get: vi.fn(),
    post: mocks.post,
    put: mocks.put,
    delete: vi.fn(),
  },
}))

import { transactionApi } from '@/features/transactions/api/transaction-api'

const request: TransactionRequest = {
  kind: 'EXPENSE',
  description: 'Lunch',
  amount: '12.34',
  categoryId: 'category-food',
  eventDate: '2026-08-19',
  installmentCount: 1,
}

describe('transactionApi writes', () => {
  beforeEach(() => {
    mocks.post.mockResolvedValue({})
    mocks.put.mockResolvedValue({})
  })

  it('creates using the exact production request through POST', async () => {
    const signal = new AbortController().signal

    await transactionApi.create(request, signal)

    expect(mocks.post).toHaveBeenCalledWith('/api/v1/transactions', request, signal)
  })

  it('edits using PUT with a full replacement request', async () => {
    const signal = new AbortController().signal

    await transactionApi.update('transaction-1', request, signal)

    expect(mocks.put).toHaveBeenCalledWith(
      '/api/v1/transactions/transaction-1',
      request,
      signal,
    )
  })
})
