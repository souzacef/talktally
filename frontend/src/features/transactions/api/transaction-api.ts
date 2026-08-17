import { apiClient } from '@/lib/api/api-client'
import { withQuery } from '@/lib/api/query-string'
import type {
  PageResponse,
  TransactionListParams,
  TransactionRequest,
  TransactionResponse,
} from '@/types/api'

const BASE_PATH = '/api/v1/transactions'

export const transactionApi = {
  list: (params: TransactionListParams = {}, signal?: AbortSignal) =>
    apiClient.get<PageResponse<TransactionResponse>>(
      withQuery(BASE_PATH, params),
      { signal },
    ),
  get: (id: string, signal?: AbortSignal) =>
    apiClient.get<TransactionResponse>(`${BASE_PATH}/${id}`, { signal }),
  create: (request: TransactionRequest, signal?: AbortSignal) =>
    apiClient.post<TransactionResponse>(BASE_PATH, request, signal),
  update: (id: string, request: TransactionRequest, signal?: AbortSignal) =>
    apiClient.put<TransactionResponse>(`${BASE_PATH}/${id}`, request, signal),
  delete: (id: string, signal?: AbortSignal) =>
    apiClient.delete(`${BASE_PATH}/${id}`, signal),
}
