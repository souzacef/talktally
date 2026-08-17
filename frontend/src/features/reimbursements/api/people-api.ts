import { apiClient } from '@/lib/api/api-client'
import type {
  PersonReimbursementSummaryResponse,
  PersonRequest,
  PersonResponse,
} from '@/types/api'

const BASE_PATH = '/api/v1/people'

export const peopleApi = {
  create: (request: PersonRequest, signal?: AbortSignal) =>
    apiClient.post<PersonResponse>(BASE_PATH, request, signal),
  list: (signal?: AbortSignal) =>
    apiClient.get<PersonResponse[]>(BASE_PATH, { signal }),
  reimbursementSummary: (personId: string, signal?: AbortSignal) =>
    apiClient.get<PersonReimbursementSummaryResponse>(
      `${BASE_PATH}/${personId}/reimbursements/summary`,
      { signal },
    ),
}
