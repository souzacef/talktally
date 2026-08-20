import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LOCALE_KEY, LocaleProvider } from '@/app/providers/locale-provider'
import { OwedPage } from '@/pages/owed-page'

const mocks = vi.hoisted(() => ({
  summary: vi.fn(),
  peopleList: vi.fn(),
  personSummary: vi.fn(),
  reimbursementList: vi.fn(),
  recordPayment: vi.fn(),
}))

vi.mock('@/features/dashboard/hooks/use-dashboard', () => ({
  useFinancialSummary: mocks.summary,
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

function renderOwed(locale: 'en-US' | 'pt-BR' = 'en-US') {
  window.localStorage.setItem(LOCALE_KEY, locale)
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  render(
    <QueryClientProvider client={client}>
      <LocaleProvider><OwedPage /></LocaleProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
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
    mocks.reimbursementList.mockResolvedValue({
      items: [{
        id: 'claim-id',
        expenseTransactionId: 'expense-id',
        personId: 'ana-id',
        personDisplayName: 'Ana Silva',
        originalAmount: '100.00',
        amountReimbursed: '19.50',
        remainingAmount: '80.50',
        currency: 'BRL',
        status: 'PARTIALLY_PAID',
        note: null,
        payments: [{
          id: 'payment-id',
          amount: '19.50',
          currency: 'BRL',
          receivedDate: '2026-08-19',
          receiptTransactionId: 'receipt-id',
          note: 'Pix',
        }],
      }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    mocks.recordPayment.mockResolvedValue({})
  })

  it('renders pt-BR copy, status, money, and payment dates', async () => {
    renderOwed('pt-BR')

    expect(screen.getByRole('heading', { name: 'A receber' })).toBeInTheDocument()
    expect((await screen.findAllByText(/R\$\s*80,50/)).length).toBeGreaterThan(0)
    const claim = (await screen.findByText('Cobrança de reembolso')).closest('article')
    expect(claim).not.toBeNull()
    expect(within(claim!).getByText('PARCIALMENTE PAGO')).toBeInTheDocument()
    expect(within(claim!).getByText(/19\/08\/2026 · Pix/)).toBeInTheDocument()
  })

  it('normalizes a comma repayment amount before sending it to the API', async () => {
    const user = renderOwed('pt-BR')
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
})
