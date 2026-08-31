import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LOCALE_KEY, LocaleProvider } from '@/app/providers/locale-provider'
import { queryKeys } from '@/lib/query/query-client'
import { OwedPage } from '@/pages/owed-page'
import type {
  PageResponse,
  ReimbursementClaimResponse,
  ReimbursementListParams,
} from '@/types/api'

const mocks = vi.hoisted(() => ({
  summary: vi.fn(),
  categoryList: vi.fn(),
  peopleList: vi.fn(),
  personSummary: vi.fn(),
  reimbursementList: vi.fn(),
  recordPayment: vi.fn(),
}))

vi.mock('@/features/dashboard/hooks/use-dashboard', () => ({
  useFinancialSummary: mocks.summary,
}))
vi.mock('@/features/categories/api/category-api', () => ({
  categoryApi: { list: mocks.categoryList },
}))
vi.mock('@/features/reimbursements/api/people-api', () => ({
  peopleApi: {
    list: mocks.peopleList,
    reimbursementSummary: mocks.personSummary,
  },
}))
vi.mock('@/features/reimbursements/api/reimbursement-api', () => ({
  reimbursementApi: {
    list: mocks.reimbursementList,
    recordPayment: mocks.recordPayment,
  },
}))

function reimbursementClaim(id = 'claim-id'): ReimbursementClaimResponse {
  return {
    id,
    expenseTransactionId: `expense-${id}`,
    sourceExpense: {
      transactionId: `expense-${id}`,
      description: id === 'claim-id' ? 'Dinner at Outback' : `Expense ${id}`,
      amount: '200.00',
      currency: 'BRL',
      categoryId: 'food-id',
      eventDate: '2026-08-14',
      firstOccurrenceDate: '2026-09-10',
      installmentCount: 3,
    },
    personId: 'ana-id',
    personDisplayName: 'Ana Silva',
    originalAmount: '100.00',
    amountReimbursed: '19.50',
    remainingAmount: '80.50',
    currency: 'BRL',
    status: 'PARTIALLY_PAID',
    note: null,
    payments: [{
      id: `payment-${id}`,
      amount: '19.50',
      currency: 'BRL',
      receivedDate: '2026-08-19',
      receiptTransactionId: `receipt-${id}`,
      note: 'Pix',
    }],
  }
}

function reimbursementPage(
  items: ReimbursementClaimResponse[],
  page = 0,
  totalPages = 1,
  totalElements = items.length,
): PageResponse<ReimbursementClaimResponse> {
  return {
    items,
    page,
    size: 20,
    totalElements,
    totalPages,
  }
}

function renderOwed(locale: 'en-US' | 'pt-BR' = 'en-US') {
  window.localStorage.setItem(LOCALE_KEY, locale)
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  const invalidate = vi.spyOn(client, 'invalidateQueries')
  render(
    <QueryClientProvider client={client}>
      <LocaleProvider><OwedPage /></LocaleProvider>
    </QueryClientProvider>,
  )
  return { invalidate, user: userEvent.setup() }
}

describe('OwedPage localization', () => {
  beforeEach(() => {
    mocks.summary.mockReturnValue({
      isPending: false,
      error: null,
      data: {
        owedToMe: { outstanding: '80.50', openClaims: 1 },
      },
    })
    mocks.categoryList.mockResolvedValue([
      { id: 'food-id', code: 'FOOD_DINING', displayName: 'Food and dining', allowedKind: 'EXPENSE', builtIn: true },
    ])
    mocks.peopleList.mockResolvedValue([{ id: 'ana-id', displayName: 'Ana Silva' }])
    mocks.personSummary.mockResolvedValue({
      personId: 'ana-id',
      displayName: 'Ana Silva',
      totalOriginal: '100.00',
      totalReimbursed: '19.50',
      totalOutstanding: '80.50',
      currency: 'BRL',
      openClaimCount: 1,
    })
    mocks.reimbursementList.mockResolvedValue(reimbursementPage([reimbursementClaim()]))
    mocks.recordPayment.mockResolvedValue({})
  })

  it.each([
    ['en-US', 0, 'Open claims', 'People'],
    ['en-US', 1, 'Open claim', 'Person'],
    ['en-US', 2, 'Open claims', 'People'],
    ['pt-BR', 0, 'Cobranças abertas', 'Pessoas'],
    ['pt-BR', 1, 'Cobrança aberta', 'Pessoa'],
    ['pt-BR', 2, 'Cobranças abertas', 'Pessoas'],
  ] as const)(
    'pluralizes summary count labels in %s for %i',
    async (locale, count, openClaimsLabel, peopleLabel) => {
      mocks.summary.mockReturnValue({
        isPending: false,
        error: null,
        data: { owedToMe: { outstanding: '80.50', openClaims: count } },
      })
      mocks.peopleList.mockResolvedValue(Array.from({ length: count }, (_, index) => ({
        id: `person-${index}`,
        displayName: `Person ${index}`,
      })))

      renderOwed(locale)

      const openClaims = screen.getByText(openClaimsLabel)
      expect(openClaims.previousElementSibling).toHaveTextContent(String(count))
      await waitFor(() => {
        const people = screen.getAllByText(peopleLabel).find(
          (element) => element.previousElementSibling?.textContent === String(count),
        )
        expect(people).toBeDefined()
      })
    },
  )

  it.each([
    ['en-US', 0, '0 open claims'],
    ['en-US', 1, '1 open claim'],
    ['en-US', 2, '2 open claims'],
    ['pt-BR', 0, '0 cobranças abertas'],
    ['pt-BR', 1, '1 cobrança aberta'],
    ['pt-BR', 2, '2 cobranças abertas'],
  ] as const)(
    'pluralizes each person open-claim summary in %s for %i',
    async (locale, count, expected) => {
      mocks.personSummary.mockResolvedValue({
        personId: 'ana-id',
        displayName: 'Ana Silva',
        totalOriginal: '100.00',
        totalReimbursed: '19.50',
        totalOutstanding: '80.50',
        currency: 'BRL',
        openClaimCount: count,
      })

      renderOwed(locale)

      expect(await screen.findByText(expected)).toBeInTheDocument()
    },
  )

  it('distinguishes the authoritative source expense from the amount originally owed', async () => {
    renderOwed()

    const claim = (await screen.findByText('Reimbursement claim')).closest('article')
    expect(claim).not.toBeNull()
    expect(within(claim!).getByRole('heading', { name: 'Dinner at Outback' })).toBeInTheDocument()
    expect(within(claim!).getByText(/Food and dining · 08\/14\/2026 · 3 installments/)).toBeInTheDocument()
    expect(within(claim!).getByText('First cash-flow date: 09/10/2026')).toBeInTheDocument()
    const sourceLabels = within(claim!).getAllByText('Source expense')
    expect(sourceLabels).toHaveLength(2)
    expect(sourceLabels[1]?.parentElement).toHaveTextContent(/R\$\s*200\.00/)
    const owedLabel = within(claim!).getByText('Originally owed')
    expect(owedLabel.parentElement).toHaveTextContent(/R\$\s*100\.00/)
    expect(within(claim!).getByText(/08\/19\/2026 · Pix/)).toBeInTheDocument()
    expect(mocks.reimbursementList).toHaveBeenCalledWith(
      { page: 0, size: 20 },
      expect.anything(),
    )
    expect(screen.queryByRole('navigation', { name: 'Reimbursement claim pages' })).not.toBeInTheDocument()
  })

  it('navigates every claims page with bounded zero-based API requests', async () => {
    mocks.reimbursementList.mockImplementation((params: ReimbursementListParams) => {
      const page = params.page ?? 0
      return Promise.resolve(reimbursementPage(
        [reimbursementClaim(`claim-${page}`)],
        page,
        3,
        45,
      ))
    })
    const { user } = renderOwed()

    const previous = await screen.findByRole('button', { name: 'Previous' })
    const next = screen.getByRole('button', { name: 'Next' })
    expect(previous).toBeDisabled()
    expect(next).toBeEnabled()
    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument()

    await user.click(next)
    await waitFor(() => expect(mocks.reimbursementList).toHaveBeenLastCalledWith(
      { page: 1, size: 20 },
      expect.anything(),
    ))
    expect(await screen.findByText('Page 2 of 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()

    await user.click(screen.getByRole('button', { name: 'Previous' }))
    await waitFor(() => expect(mocks.reimbursementList).toHaveBeenLastCalledWith(
      { page: 0, size: 20 },
      expect.anything(),
    ))
    expect(await screen.findByText('Page 1 of 3')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Next' }))
    expect(await screen.findByText('Page 2 of 3')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Next' }))
    await waitFor(() => expect(mocks.reimbursementList).toHaveBeenLastCalledWith(
      { page: 2, size: 20 },
      expect.anything(),
    ))
    expect(await screen.findByText('Page 3 of 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('renders pt-BR copy, status, money, and payment dates', async () => {
    mocks.reimbursementList.mockResolvedValue(reimbursementPage(
      [reimbursementClaim()],
      0,
      3,
      45,
    ))
    renderOwed('pt-BR')

    expect(screen.getByRole('heading', { name: 'A receber' })).toBeInTheDocument()
    expect((await screen.findAllByText(/R\$\s*80,50/)).length).toBeGreaterThan(0)
    const claim = (await screen.findByText('Cobrança de reembolso')).closest('article')
    expect(claim).not.toBeNull()
    expect(within(claim!).getByText('Parcialmente pago')).toBeInTheDocument()
    expect(within(claim!).getByRole('heading', { name: 'Dinner at Outback' })).toBeInTheDocument()
    expect(within(claim!).getByText(/Alimentação · 14\/08\/2026 · 3 parcelas/)).toBeInTheDocument()
    expect(within(claim!).getByText('Primeiro fluxo de caixa: 10/09/2026')).toBeInTheDocument()
    expect(within(claim!).getByText('Valor originalmente devido')).toBeInTheDocument()
    expect(within(claim!).queryByText('Valor original')).not.toBeInTheDocument()
    expect(within(claim!).getByText(/19\/08\/2026 · Pix/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()
    expect(screen.getByText('Página 1 de 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Próxima' })).toBeEnabled()
  })

  it('normalizes a comma repayment amount before sending it to the API', async () => {
    const { user } = renderOwed('pt-BR')
    const recordButton = await screen.findByRole('button', { name: 'Registrar pagamento' })
    await user.click(recordButton)
    await user.type(screen.getByLabelText('Valor'), '12,34')
    await user.clear(screen.getByLabelText('Data de recebimento'))
    await user.type(screen.getByLabelText('Data de recebimento'), '2026-08-20')
    await user.click(screen.getByRole('button', { name: 'Registrar pagamento' }))

    expect(mocks.recordPayment).toHaveBeenCalledWith('claim-id', {
      amount: '12.34',
      receivedDate: '2026-08-20',
      note: null,
    })
  })

  it('records repayment on the visible page and preserves that page after invalidation', async () => {
    mocks.reimbursementList.mockImplementation((params: ReimbursementListParams) => {
      const page = params.page ?? 0
      return Promise.resolve(reimbursementPage(
        [reimbursementClaim(`claim-${page}`)],
        page,
        2,
        25,
      ))
    })
    const { invalidate, user } = renderOwed()
    await user.click(await screen.findByRole('button', { name: 'Next' }))
    expect(await screen.findByText('Page 2 of 2')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Record repayment' }))
    await user.type(screen.getByLabelText('Amount'), '10.00')
    await user.clear(screen.getByLabelText('Received date'))
    await user.type(screen.getByLabelText('Received date'), '2026-08-21')
    await user.click(screen.getByRole('button', { name: 'Record repayment' }))

    await waitFor(() => expect(mocks.recordPayment).toHaveBeenCalledWith('claim-1', {
      amount: '10.00',
      receivedDate: '2026-08-21',
      note: null,
    }))
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.reimbursements.all })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.people.all })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.dashboard.all })
    await waitFor(() => expect(mocks.reimbursementList).toHaveBeenLastCalledWith(
      { page: 1, size: 20 },
      expect.anything(),
    ))
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
  })

  it('keeps global dashboard and people summaries independent from page item count', async () => {
    mocks.summary.mockReturnValue({
      isPending: false,
      error: null,
      data: {
        owedToMe: { outstanding: '999.99', openClaims: 35 },
      },
    })
    mocks.personSummary.mockResolvedValue({
      personId: 'ana-id',
      displayName: 'Ana Silva',
      totalOriginal: '700.00',
      totalReimbursed: '144.45',
      totalOutstanding: '555.55',
      currency: 'BRL',
      openClaimCount: 12,
    })
    mocks.reimbursementList.mockResolvedValue(reimbursementPage(
      [reimbursementClaim()],
      0,
      2,
      35,
    ))
    renderOwed()

    expect((await screen.findAllByText(/R\$\s*999\.99/)).length).toBeGreaterThan(0)
    expect(screen.getByText('35')).toBeInTheDocument()
    expect(await screen.findByText('12 open claims')).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*555\.55/)).toBeInTheDocument()
    expect(screen.getAllByText('Reimbursement claim')).toHaveLength(1)
  })

  it('shows the normal empty state without pagination for a zero-page response', async () => {
    mocks.reimbursementList.mockResolvedValue(reimbursementPage([], 0, 0, 0))
    renderOwed()

    expect(await screen.findByText('No reimbursement claims')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Reimbursement claim pages' })).not.toBeInTheDocument()
    expect(mocks.reimbursementList).toHaveBeenCalledWith(
      { page: 0, size: 20 },
      expect.anything(),
    )
  })

  it('settles an externally out-of-range page on the last valid page without looping', async () => {
    let contracted = false
    mocks.reimbursementList.mockImplementation((params: ReimbursementListParams) => {
      const page = params.page ?? 0
      if (page === 2) {
        contracted = true
        return Promise.resolve(reimbursementPage([], 2, 2, 21))
      }
      return Promise.resolve(reimbursementPage(
        [reimbursementClaim(`claim-${page}`)],
        page,
        contracted ? 2 : 3,
        contracted ? 21 : 45,
      ))
    })
    const { user } = renderOwed()

    await user.click(await screen.findByRole('button', { name: 'Next' }))
    expect(await screen.findByText('Page 2 of 3')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByText('Page 2 of 2')).toBeInTheDocument()
    await waitFor(() => expect(mocks.reimbursementList).toHaveBeenCalledTimes(4))
    expect(mocks.reimbursementList).toHaveBeenLastCalledWith(
      { page: 1, size: 20 },
      expect.anything(),
    )
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })
})
