import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DashboardPage } from '@/pages/dashboard-page'
import { LOCALE_KEY, LocaleProvider } from '@/app/providers/locale-provider'
import { AuthProvider } from '@/features/auth/auth-provider'
import { AuthSession } from '@/lib/auth/auth-session'
import type { TransactionResponse } from '@/types/api'

const mocks = vi.hoisted(() => ({
  summary: vi.fn(),
  categoryBreakdown: vi.fn(),
  monthlyCashFlow: vi.fn(),
  transactionList: vi.fn(),
  categoryList: vi.fn(),
  peopleList: vi.fn(),
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
  peopleApi: { list: mocks.peopleList },
}))

const recentTransaction: TransactionResponse = {
  id: 'recent-transaction-id',
  kind: 'EXPENSE',
  description: 'Recent coffee',
  amount: '7.89',
  currency: 'BRL',
  categoryId: 'food-category-id',
  eventDate: '2026-08-19',
  firstOccurrenceDate: '2026-08-19',
  source: 'MANUAL',
  installmentCount: 1,
  occurrences: [
    { sequenceNumber: 1, effectiveDate: '2026-08-19', amount: '7.89', currency: 'BRL' },
  ],
}

function renderDashboard(displayName = 'Carlos Eduardo Freire de Souza') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const session = new AuthSession(window.sessionStorage)
  session.setAuthenticated('test-token', {
    userId: 'user-id',
    email: 'carlos@example.com',
    displayName,
    defaultCurrency: 'BRL',
  })
  render(
    <QueryClientProvider client={client}>
      <LocaleProvider>
        <AuthProvider session={session} privateQueryClient={client}>
          <MemoryRouter initialEntries={['/dashboard']}>
            <Routes>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/transactions" element={<p>All transactions destination</p>} />
              <Route path="/transactions/:transactionId" element={<p>Transaction detail destination</p>} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </LocaleProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

describe('Dashboard Recent Activity navigation', () => {
  beforeEach(() => {
    mocks.summary.mockReturnValue({
      isPending: false,
      error: null,
      data: {
        period: {
          earnedIncome: '0',
          expenses: '7.89',
          reimbursementsReceived: '0',
          netCashFlow: '-7.89',
        },
        owedToMe: { outstanding: '0', openClaims: 0 },
      },
    })
    mocks.categoryBreakdown.mockReturnValue({
      isPending: false,
      error: null,
      data: { categories: [] },
    })
    mocks.monthlyCashFlow.mockReturnValue({
      isPending: false,
      error: null,
      data: { buckets: [] },
    })
    mocks.transactionList.mockResolvedValue({
      items: [recentTransaction],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    })
    mocks.categoryList.mockResolvedValue([
      { id: 'food-category-id', code: 'FOOD_DINING', displayName: 'Food and dining', allowedKind: 'EXPENSE', builtIn: true },
    ])
    mocks.peopleList.mockResolvedValue([])
  })

  it('navigates View all to the transaction ledger', async () => {
    const user = renderDashboard()

    await user.click(await screen.findByRole('link', { name: 'View all' }))
    expect(screen.getByText('All transactions destination')).toBeInTheDocument()
  })

  it('greets the authenticated user by first name', () => {
    renderDashboard()

    expect(screen.getByRole('heading', { name: 'Hello, Carlos!' })).toBeInTheDocument()
  })

  it('shows the friendly category and navigates a recent transaction to detail', async () => {
    const user = renderDashboard()

    expect(await screen.findByText(/Food and dining/)).toBeInTheDocument()
    expect(document.body.textContent).not.toContain('food-category-id')
    await user.click(screen.getByRole('link', { name: 'View transaction Recent coffee' }))
    expect(screen.getByText('Transaction detail destination')).toBeInTheDocument()
  })

  it('renders dashboard copy, category labels, dates, and BRL amounts in pt-BR', async () => {
    window.localStorage.setItem(LOCALE_KEY, 'pt-BR')
    renderDashboard()

    expect(screen.getByRole('heading', { name: 'Olá, Carlos!' })).toBeInTheDocument()
    expect(screen.getByText('Atividade recente')).toBeInTheDocument()
    expect(await screen.findByText(/Alimentação · 19\/08\/2026/)).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*7,89/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver transação Recent coffee' })).toBeInTheDocument()
  })
})
