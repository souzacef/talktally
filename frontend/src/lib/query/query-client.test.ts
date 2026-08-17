import { describe, expect, it } from 'vitest'
import { queryClient } from '@/lib/query/query-client'

describe('queryClient', () => {
  it('never retries mutations that could duplicate writes', () => {
    expect(queryClient.getDefaultOptions().mutations?.retry).toBe(false)
  })
})
