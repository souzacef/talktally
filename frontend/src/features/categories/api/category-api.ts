import { apiClient } from '@/lib/api/api-client'
import type { Category } from '@/types/api'

const BASE_PATH = '/api/v1/categories'

export const categoryApi = {
  list: (signal?: AbortSignal) =>
    apiClient.get<Category[]>(BASE_PATH, { signal }),
}
