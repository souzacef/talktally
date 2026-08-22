import { describe, expect, it, vi } from 'vitest'
import { createBackendHealthApi } from '@/features/health/backend-health-api'
import { environment } from '@/lib/env/env'

describe('backendHealthApi', () => {
  it('requests the configured Actuator health URL as JSON and exposes only up/down', async () => {
    const fetchImplementation = vi.fn<typeof fetch>().mockResolvedValue(new Response(
      JSON.stringify({ status: 'UP', components: { db: { details: { secret: true } } } }),
      { status: 200, headers: { 'Content-Type': 'application/vnd.spring-boot.actuator.v3+json' } },
    ))

    await expect(createBackendHealthApi(fetchImplementation).isUp()).resolves.toBe(true)

    expect(fetchImplementation).toHaveBeenCalledWith(
      `${environment.apiBaseUrl}/actuator/health`,
      expect.objectContaining({
        method: 'GET',
        headers: expect.any(Headers),
      }),
    )
    const request = fetchImplementation.mock.calls[0]?.[1]
    expect(new Headers(request?.headers).get('Accept')).toBe('application/json')
  })
})
