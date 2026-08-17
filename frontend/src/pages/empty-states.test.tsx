import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DashboardPage } from '@/pages/dashboard-page'
import { OwedPage } from '@/pages/owed-page'
import { TransactionsPage } from '@/pages/transactions-page'

const mocks = vi.hoisted(() => ({
  summary: vi.fn(),
  categoryBreakdown: vi.fn(),
  monthlyCashFlow: vi.fn(),
  transactionList: vi.fn(),
  peopleList: vi.fn(),
  personSummary: vi.fn(),
  reimbursementList: vi.fn(),
  recordPayment: vi.fn(),
}))

vi.mock('@/features/dashboard/hooks/use-dashboard', () => ({
  useFinancialSummary: mocks.summary,
  useCategoryBreakdown: mocks.categoryBreakdown,
  useMonthlyCashFlow: mocks.monthlyCashFlow,
}))
vi.mock('@/features/transactions/api/transaction-api', () => ({
  transactionApi: { list: mocks.transactionList },
}))
vi.mock('@/features/reimbursements/api/people-api', () => ({
  peopleApi: { list: mocks.peopleList, reimbursementSummary: mocks.personSummary },
}))
vi.mock('@/features/reimbursements/api/reimbursement-api', () => ({
  reimbursementApi: { list: mocks.reimbursementList, recordPayment: mocks.recordPayment },
}))

function renderPage(page: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>{page}</MemoryRouter>
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
