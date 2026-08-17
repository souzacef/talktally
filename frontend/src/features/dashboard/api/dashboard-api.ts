import { apiClient } from '@/lib/api/api-client'
import { withQuery } from '@/lib/api/query-string'
import type {
  CategoryBreakdownResponse,
  FinancialSummaryResponse,
  MonthlyCashFlowResponse,
  TransactionKind,
} from '@/types/api'

const BASE_PATH = '/api/v1/dashboard'

export const dashboardApi = {
  summary: (from: string, to: string, signal?: AbortSignal) =>
    apiClient.get<FinancialSummaryResponse>(
      withQuery(`${BASE_PATH}/summary`, { from, to }),
      { signal },
    ),
  categoryBreakdown: (
    from: string,
    to: string,
    kind: TransactionKind,
    signal?: AbortSignal,
  ) => apiClient.get<CategoryBreakdownResponse>(
    withQuery(`${BASE_PATH}/category-breakdown`, { from, to, kind }),
    { signal },
  ),
  monthlyCashFlow: (from: string, to: string, signal?: AbortSignal) =>
    apiClient.get<MonthlyCashFlowResponse>(
      withQuery(`${BASE_PATH}/monthly-cash-flow`, { from, to }),
      { signal },
    ),
}
