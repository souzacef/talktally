import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '@/features/dashboard/api/dashboard-api'
import { queryKeys } from '@/lib/query/query-client'
import type { TransactionKind } from '@/types/api'

export function useFinancialSummary(from: string, to: string) {
  return useQuery({
    queryKey: queryKeys.dashboard.summary(from, to),
    queryFn: ({ signal }) => dashboardApi.summary(from, to, signal),
  })
}

export function useCategoryBreakdown(
  from: string,
  to: string,
  kind: TransactionKind,
) {
  return useQuery({
    queryKey: queryKeys.dashboard.categoryBreakdown(from, to, kind),
    queryFn: ({ signal }) => dashboardApi.categoryBreakdown(from, to, kind, signal),
  })
}

export function useMonthlyCashFlow(from: string, to: string) {
  return useQuery({
    queryKey: queryKeys.dashboard.monthlyCashFlow(from, to),
    queryFn: ({ signal }) => dashboardApi.monthlyCashFlow(from, to, signal),
  })
}
