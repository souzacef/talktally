import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '@/lib/api/api-client'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
          return false
        }
        return failureCount < 2
      },
    },
    mutations: {
      retry: false,
    },
  },
})

export const queryKeys = {
  categories: {
    all: ['categories'] as const,
  },
  dashboard: {
    all: ['dashboard'] as const,
    summary: (from: string, to: string) => ['dashboard', 'summary', from, to] as const,
    categoryBreakdown: (from: string, to: string, kind: string) =>
      ['dashboard', 'category-breakdown', from, to, kind] as const,
    monthlyCashFlow: (from: string, to: string) =>
      ['dashboard', 'monthly-cash-flow', from, to] as const,
  },
  transactions: {
    all: ['transactions'] as const,
    list: (params: object) => ['transactions', 'list', params] as const,
    detail: (id: string) => ['transactions', 'detail', id] as const,
  },
  people: {
    all: ['people'] as const,
    detail: (id: string) => ['people', id] as const,
  },
  reimbursements: {
    all: ['reimbursements'] as const,
    list: (params: object) => ['reimbursements', 'list', params] as const,
    detail: (id: string) => ['reimbursements', 'detail', id] as const,
  },
}
