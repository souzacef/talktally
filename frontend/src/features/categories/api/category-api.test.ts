import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
}))

vi.mock('@/lib/api/api-client', () => ({
  apiClient: { get: mocks.get },
}))

import { categoryApi } from '@/features/categories/api/category-api'

describe('categoryApi', () => {
  beforeEach(() => {
    mocks.get.mockResolvedValue([])
  })

  it('fetches the catalog through the shared authenticated API client', async () => {
    const signal = new AbortController().signal

    await categoryApi.list(signal)

    expect(mocks.get).toHaveBeenCalledWith('/api/v1/categories', { signal })
  })
})
