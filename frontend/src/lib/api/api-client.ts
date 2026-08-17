import { authSession } from '@/lib/auth/auth-session'
import { environment, normalizeApiBaseUrl } from '@/lib/env/env'
import type { ApiErrorPayload } from '@/types/api'

type FetchImplementation = typeof fetch

export interface ApiClientOptions {
  baseUrl: string
  getAccessToken?: () => string | null
  onUnauthorized?: () => void
  fetchImplementation?: FetchImplementation
}

export interface ApiRequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  json?: unknown
  formData?: FormData
  signal?: AbortSignal
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: ApiErrorPayload

  constructor(
    status: number,
    code: string,
    message: string,
    details: ApiErrorPayload = {},
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }
}

function isErrorPayload(value: unknown): value is ApiErrorPayload {
  return typeof value === 'object' && value !== null
}

export class ApiClient {
  readonly baseUrl: string
  private readonly getAccessToken: () => string | null
  private readonly onUnauthorized: () => void
  private readonly fetchImplementation: FetchImplementation

  constructor(options: ApiClientOptions) {
    this.baseUrl = normalizeApiBaseUrl(options.baseUrl)
    this.getAccessToken = options.getAccessToken ?? (() => null)
    this.onUnauthorized = options.onUnauthorized ?? (() => undefined)
    this.fetchImplementation = options.fetchImplementation ?? fetch
  }

  get<T>(path: string, options: Omit<ApiRequestOptions, 'method' | 'json' | 'formData'> = {}) {
    return this.request<T>(path, { ...options, method: 'GET' })
  }

  post<T>(path: string, json: unknown, signal?: AbortSignal) {
    return this.request<T>(path, { method: 'POST', json, signal })
  }

  postForm<T>(path: string, formData: FormData, signal?: AbortSignal) {
    return this.request<T>(path, { method: 'POST', formData, signal })
  }

  put<T>(path: string, json: unknown, signal?: AbortSignal) {
    return this.request<T>(path, { method: 'PUT', json, signal })
  }

  delete(path: string, signal?: AbortSignal) {
    return this.request<void>(path, { method: 'DELETE', signal })
  }

  async request<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
    if (!path.startsWith('/')) {
      throw new Error('API paths must start with /')
    }
    if (options.json !== undefined && options.formData) {
      throw new Error('A request cannot contain both JSON and FormData')
    }

    const headers = new Headers({ Accept: 'application/json' })
    const token = this.getAccessToken()
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }

    let body: BodyInit | undefined
    if (options.formData) {
      body = options.formData
    } else if (options.json !== undefined) {
      headers.set('Content-Type', 'application/json')
      body = JSON.stringify(options.json)
    }

    let response: Response
    try {
      response = await this.fetchImplementation(`${this.baseUrl}${path}`, {
        method: options.method ?? 'GET',
        headers,
        body,
        signal: options.signal,
      })
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        throw error
      }
      throw new ApiError(0, 'NETWORK_ERROR', 'Unable to reach TalkTally')
    }

    const responseText = response.status === 204 ? '' : await response.text()
    let payload: unknown
    if (responseText) {
      try {
        payload = JSON.parse(responseText)
      } catch {
        payload = undefined
      }
    }

    if (!response.ok) {
      if (response.status === 401) {
        this.onUnauthorized()
      }
      const details = isErrorPayload(payload) ? payload : {}
      const message = details.message ?? details.detail ?? (response.statusText || 'Request failed')
      throw new ApiError(
        response.status,
        details.code ?? 'HTTP_ERROR',
        message,
        details,
      )
    }

    if (response.status === 204 || !responseText) {
      return undefined as T
    }
    return payload as T
  }
}

export const apiClient = new ApiClient({
  baseUrl: environment.apiBaseUrl,
  getAccessToken: authSession.getToken,
  onUnauthorized: authSession.expire,
})
