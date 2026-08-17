import { apiClient } from '@/lib/api/api-client'
import { withQuery } from '@/lib/api/query-string'
import type {
  CreateReimbursementRequest,
  CreateReimbursementResponse,
  PageResponse,
  RecordReimbursementPaymentResponse,
  ReimbursementClaimResponse,
  ReimbursementListParams,
  ReimbursementPaymentRequest,
} from '@/types/api'

const BASE_PATH = '/api/v1/reimbursements'

export const reimbursementApi = {
  create: (request: CreateReimbursementRequest, signal?: AbortSignal) =>
    apiClient.post<CreateReimbursementResponse>(BASE_PATH, request, signal),
  list: (params: ReimbursementListParams = {}, signal?: AbortSignal) =>
    apiClient.get<PageResponse<ReimbursementClaimResponse>>(
      withQuery(BASE_PATH, params),
      { signal },
    ),
  get: (claimId: string, signal?: AbortSignal) =>
    apiClient.get<ReimbursementClaimResponse>(`${BASE_PATH}/${claimId}`, { signal }),
  recordPayment: (
    claimId: string,
    request: ReimbursementPaymentRequest,
    signal?: AbortSignal,
  ) => apiClient.post<RecordReimbursementPaymentResponse>(
    `${BASE_PATH}/${claimId}/payments`,
    request,
    signal,
  ),
}
