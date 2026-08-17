const LOCAL_API_URL = 'http://localhost:8080'

export function normalizeApiBaseUrl(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) {
    throw new Error('VITE_API_BASE_URL is required')
  }

  let url: URL
  try {
    url = new URL(trimmed)
  } catch {
    throw new Error('VITE_API_BASE_URL must be a valid absolute URL')
  }

  if (!['http:', 'https:'].includes(url.protocol)) {
    throw new Error('VITE_API_BASE_URL must use http or https')
  }
  if (url.username || url.password || url.search || url.hash) {
    throw new Error('VITE_API_BASE_URL must not contain credentials, query, or fragment')
  }

  return url.toString().replace(/\/+$/, '')
}

export function readEnvironment(source: Record<string, unknown>) {
  const configured = source.VITE_API_BASE_URL
  const isTest = source.MODE === 'test'
  if (typeof configured !== 'string' && !isTest) {
    throw new Error('VITE_API_BASE_URL is required')
  }
  return {
    apiBaseUrl: normalizeApiBaseUrl(
      typeof configured === 'string' ? configured : LOCAL_API_URL,
    ),
  }
}

export const environment = readEnvironment(import.meta.env)
