import { ApiClient } from '@/lib/api/api-client'
import { environment } from '@/lib/env/env'

interface HealthResponse {
  status?: unknown
}

export function createBackendHealthApi(fetchImplementation?: typeof fetch) {
  const healthClient = new ApiClient({
    baseUrl: environment.apiBaseUrl,
    fetchImplementation,
  })
  return {
    async isUp(signal?: AbortSignal): Promise<boolean> {
      const response = await healthClient.get<HealthResponse>('/actuator/health', { signal })
      return response?.status === 'UP'
    },
  }
}

export const backendHealthApi = createBackendHealthApi()
