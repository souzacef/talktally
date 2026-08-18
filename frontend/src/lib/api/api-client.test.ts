import { describe, expect, it, vi } from 'vitest'
import { ApiClient, ApiError } from '@/lib/api/api-client'
import { AuthSession } from '@/lib/auth/auth-session'
import { normalizeApiBaseUrl } from '@/lib/env/env'

function clientWith(fetchMock: ReturnType<typeof vi.fn>, options: {
  token?: string | null
  onUnauthorized?: () => void
} = {}) {
  return new ApiClient({
    baseUrl: 'http://localhost:8080/',
    getAccessToken: () => options.token ?? null,
    onUnauthorized: options.onUnauthorized,
    fetchImplementation: fetchMock as unknown as typeof fetch,
  })
}

describe('ApiClient', () => {
  it('preserves the native fetch receiver on the default-fetch path', async () => {
    const receiverSensitiveFetch = vi.fn(function (this: typeof globalThis) {
      if (this !== globalThis) throw new TypeError('Illegal invocation')
      return Promise.resolve(new Response('{"ok":true}', { status: 200 }))
    })
    vi.stubGlobal('fetch', receiverSensitiveFetch)

    try {
      const client = new ApiClient({ baseUrl: 'http://localhost:8080' })
      await expect(client.get('/api/test')).resolves.toEqual({ ok: true })
      expect(receiverSensitiveFetch).toHaveBeenCalledOnce()
    } finally {
      vi.unstubAllGlobals()
    }
  })

  it('normalizes trailing slashes and rejects malformed base URLs', () => {
    expect(normalizeApiBaseUrl('http://localhost:8080///')).toBe('http://localhost:8080')
    expect(() => normalizeApiBaseUrl('not a url')).toThrow(/absolute URL/)
    expect(() => normalizeApiBaseUrl('file:///tmp/api')).toThrow(/http or https/)
  })

  it('injects bearer auth and serializes JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{"ok":true}', {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }))
    await clientWith(fetchMock, { token: 'test-token' }).post('/api/test', { amount: '12.34' })

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = init.headers as Headers
    expect(url).toBe('http://localhost:8080/api/test')
    expect(headers.get('Authorization')).toBe('Bearer test-token')
    expect(headers.get('Content-Type')).toBe('application/json')
    expect(init.body).toBe('{"amount":"12.34"}')
  })

  it('leaves multipart boundaries to the browser', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{}', { status: 200 }))
    const formData = new FormData()
    formData.append('file', new Blob(['wav'], { type: 'audio/wav' }), 'voice.wav')
    await clientWith(fetchMock).postForm('/api/v1/assistant/voice', formData)

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit
    expect((init.headers as Headers).has('Content-Type')).toBe(false)
    expect(init.body).toBe(formData)
  })

  it('returns undefined for a 204 response', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    await expect(clientWith(fetchMock).delete('/api/item/1')).resolves.toBeUndefined()
  })

  it('retains the backend error code and safe message', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      '{"code":"INVALID_REQUEST","message":"request validation failed"}',
      { status: 400 },
    ))
    await expect(clientWith(fetchMock).get('/api/test')).rejects.toMatchObject({
      status: 400,
      code: 'INVALID_REQUEST',
      message: 'request validation failed',
    } satisfies Partial<ApiError>)
  })

  it('clears a stale session through the centralized 401 behavior', async () => {
    const storage = window.sessionStorage
    const session = new AuthSession(storage)
    session.setToken('stale-token')
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      '{"code":"UNAUTHORIZED","message":"Authentication is required"}',
      { status: 401 },
    ))
    await expect(clientWith(fetchMock, {
      token: session.getToken(),
      onUnauthorized: session.expire,
    }).get('/api/protected')).rejects.toBeInstanceOf(ApiError)
    expect(session.getToken()).toBeNull()
  })
})
