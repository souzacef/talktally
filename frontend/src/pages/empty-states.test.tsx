import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '@/app/providers/locale-provider'
import { DashboardPage } from '@/pages/dashboard-page'
import { OwedPage } from '@/pages/owed-page'
import { TransactionsPage } from '@/pages/transactions-page'
import { AuthProvider } from '@/features/auth/auth-provider'
import { AuthSession } from '@/lib/auth/auth-session'

const mocks = vi.hoisted(() => ({
  summary: vi.fn(),
  categoryBreakdown: vi.fn(),
  monthlyCashFlow: vi.fn(),
  transactionList: vi.fn(),
  peopleList: vi.fn(),
  personSummary: vi.fn(),
  reimbursementList: vi.fn(),
  recordPayment: vi.fn(),
  categoryList: vi.fn(),
}))

vi.mock('@/features/dashboard/hooks/use-dashboard', () => ({
  useFinancialSummary: mocks.summary,
  useCategoryBreakdown: mocks.categoryBreakdown,
  useMonthlyCashFlow: mocks.monthlyCashFlow,
}))
vi.mock('@/features/transactions/api/transaction-api', () => ({
  transactionApi: { list: mocks.transactionList },
}))
vi.mock('@/features/categories/api/category-api', () => ({
  categoryApi: { list: mocks.categoryList },
}))
vi.mock('@/features/reimbursements/api/people-api', () => ({
  peopleApi: { list: mocks.peopleList, reimbursementSummary: mocks.personSummary },
}))
vi.mock('@/features/reimbursements/api/reimbursement-api', () => ({
  reimbursementApi: { list: mocks.reimbursementList, recordPayment: mocks.recordPayment },
}))

function renderPage(page: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const session = new AuthSession(window.sessionStorage)
  session.setAuthenticated('test-token', {
    userId: 'user-id',
    email: 'user@example.com',
    displayName: 'User',
    defaultCurrency: 'BRL',
  })
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider>
        <AuthProvider session={session} privateQueryClient={client}>
          <MemoryRouter>{page}</MemoryRouter>
        </AuthProvider>
      </LocaleProvider>
    </QueryClientProvider>,
  )
}

const emptyPage = { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

describe('real-data empty states', () => {
  beforeEach(() => {
    mocks.summary.mockReturnValue({
      isPending: false,
      error: null,
      data: {
        from: '2026-08-01',
        to: '2026-08-31',
        currency: 'BRL',
        period: {
          earnedIncome: '0',
          expenses: '0',
          reimbursementsReceived: '0',
          netCashFlow: '0',
          occurrenceCount: 0,
          transactionCount: 0,
        },
        owedToMe: { outstanding: '0', openClaims: 0 },
      },
    })
    mocks.categoryBreakdown.mockReturnValue({
      isPending: false,
      error: null,
      data: { from: '2026-08-01', to: '2026-08-31', kind: 'EXPENSE', currency: 'BRL', total: '0', categories: [] },
    })
    mocks.monthlyCashFlow.mockReturnValue({
      isPending: false,
      error: null,
      data: { from: '2026-03-01', to: '2026-08-31', currency: 'BRL', buckets: [] },
    })
    mocks.transactionList.mockResolvedValue(emptyPage)
    mocks.categoryList.mockResolvedValue([])
    mocks.peopleList.mockResolvedValue([])
    mocks.reimbursementList.mockResolvedValue(emptyPage)
  })

  it('shows clean dashboard empty states without seeded values', async () => {
    renderPage(<DashboardPage />)
    expect(await screen.findByText('No cash-flow activity')).toBeInTheDocument()
    expect(screen.getByText('No expenses yet')).toBeInTheDocument()
    expect(await screen.findByText('No transactions yet')).toBeInTheDocument()
  })

  it('shows a clean empty transaction ledger', async () => {
    renderPage(<TransactionsPage />)
    expect(await screen.findByText('No matching transactions')).toBeInTheDocument()
  })

  it('shows clean people and reimbursement empty states', async () => {
    renderPage(<OwedPage />)
    expect(await screen.findByText('No people yet')).toBeInTheDocument()
    expect(await screen.findByText('No reimbursement claims')).toBeInTheDocument()
  })
})
