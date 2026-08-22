import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '@/app/providers/locale-provider'
import { ApiError } from '@/lib/api/api-client'
import { queryKeys } from '@/lib/query/query-client'
import { TransactionDetailPage } from '@/pages/transaction-detail-page'
import type { Category, TransactionResponse, TransactionSource } from '@/types/api'

const mocks = vi.hoisted(() => ({
  categoryList: vi.fn(),
  get: vi.fn(),
  update: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('@/features/categories/api/category-api', () => ({
  categoryApi: { list: mocks.categoryList },
}))

vi.mock('@/features/transactions/api/transaction-api', () => ({
  transactionApi: {
    get: mocks.get,
    update: mocks.update,
    delete: mocks.delete,
  },
}))

const categories: Category[] = [
  { id: 'food-category-id', code: 'FOOD_DINING', displayName: 'Food and dining', allowedKind: 'EXPENSE', builtIn: true },
  { id: 'reimbursement-category-id', code: 'REIMBURSEMENT', displayName: 'Reimbursement', allowedKind: 'REIMBURSEMENT_RECEIPT', builtIn: true },
]

const expense: TransactionResponse = {
  id: 'transaction-42', kind: 'EXPENSE', description: 'Dinner with friends', amount: '30.06', currency: 'BRL', categoryId: 'food-category-id', eventDate: '2026-08-19', firstOccurrenceDate: '2026-08-19', source: 'ASSISTANT_TEXT', installmentCount: 1,
  managedByReimbursement: false,
  createdAt: '2026-08-22T17:35:00Z',
  updatedAt: '2026-08-22T17:35:00Z',
  occurrences: [{ sequenceNumber: 1, effectiveDate: '2026-08-19', amount: '30.06', currency: 'BRL' }],
}

const reimbursement: TransactionResponse = {
  ...expense, id: 'receipt-42', kind: 'REIMBURSEMENT_RECEIPT', description: 'Reimbursement from Ana', amount: '60.00', categoryId: 'reimbursement-category-id', source: 'VOICE',
  managedByReimbursement: true,
}

function Destination() {
  const location = useLocation()
  const state = location.state as { feedback?: string } | null
  return <p>Transaction list destination {state?.feedback}</p>
}

function renderDetail(path = '/transactions/transaction-42') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const invalidate = vi.spyOn(client, 'invalidateQueries')
  render(
    <LocaleProvider>
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/transactions/:transactionId" element={<TransactionDetailPage />} />
            <Route path="/transactions" element={<Destination />} />
            <Route path="/owed" element={<p>Owed to Me destination</p>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </LocaleProvider>,
  )
  return { invalidate, user: userEvent.setup() }
}

describe('TransactionDetailPage', () => {
  beforeEach(() => {
    mocks.categoryList.mockResolvedValue(categories)
    mocks.get.mockResolvedValue(expense)
    mocks.update.mockResolvedValue(expense)
    mocks.delete.mockResolvedValue(undefined)
  })

  it('fetches the route transaction ID and shows a loading state while it resolves', async () => {
    let resolveTransaction: ((value: TransactionResponse) => void) | undefined
    mocks.get.mockImplementation(() => new Promise<TransactionResponse>((resolve) => { resolveTransaction = resolve }))
    renderDetail('/transactions/requested-transaction')
    expect(screen.getByLabelText('Loading transaction detail')).toBeInTheDocument()
    expect(mocks.get).toHaveBeenCalledWith('requested-transaction', expect.any(AbortSignal))
    resolveTransaction?.(expense)
    expect(await screen.findByRole('heading', { name: 'Dinner with friends' })).toBeInTheDocument()
  })

  it('renders a normal not-found state for a backend 404', async () => {
    mocks.get.mockRejectedValue(new ApiError(404, 'TRANSACTION_NOT_FOUND', 'hidden backend detail'))
    renderDetail()
    expect(await screen.findByText('Transaction not found')).toBeInTheDocument()
    expect(screen.queryByText('hidden backend detail')).not.toBeInTheDocument()
  })

  it('renders consumer-facing transaction facts without exposing UUIDs or one-installment copy', async () => {
    renderDetail()
    expect(await screen.findByRole('heading', { name: 'Dinner with friends' })).toBeInTheDocument()
    expect(screen.getAllByText('Expense').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Food and dining').length).toBeGreaterThan(0)
    expect(screen.getAllByText('08/19/2026')).toHaveLength(1)
    expect(screen.queryByText('First cash-flow date')).not.toBeInTheDocument()
    expect(screen.getByText('Assistant')).toBeInTheDocument()
    expect(screen.getByText('Recorded')).toBeInTheDocument()
    expect(screen.getByText(/Aug 22, 2026/)).toBeInTheDocument()
    expect(screen.queryByText('Updated')).not.toBeInTheDocument()
    expect(screen.getByText(/R\$\s*30\.06/)).toBeInTheDocument()
    expect(screen.queryByText(/1 installment/i)).not.toBeInTheDocument()
    expect(document.body.textContent).not.toContain('food-category-id')
    expect(document.body.textContent).not.toContain('transaction-42')
  })

  it('shows the persisted updated timestamp only when it differs from creation', async () => {
    mocks.get.mockResolvedValue({
      ...expense,
      updatedAt: '2026-08-22T18:45:00Z',
    })
    renderDetail()

    expect(await screen.findByText('Recorded')).toBeInTheDocument()
    expect(screen.getByText('Updated')).toBeInTheDocument()
    expect(screen.getAllByText(/Aug 22, 2026/)).toHaveLength(2)
    expect(screen.getByText('08/19/2026')).toBeInTheDocument()
  })

  it('shows a distinct first cash-flow date for a delayed one-installment transaction', async () => {
    mocks.get.mockResolvedValue({ ...expense, firstOccurrenceDate: '2026-09-10', occurrences: [{ sequenceNumber: 1, effectiveDate: '2026-09-10', amount: '30.06', currency: 'BRL' }] })
    renderDetail()
    expect(await screen.findByText('Event date')).toBeInTheDocument()
    expect(screen.getByText('08/19/2026')).toBeInTheDocument()
    expect(screen.getByText('First cash-flow date')).toBeInTheDocument()
    expect(screen.getByText('09/10/2026')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Installments' })).not.toBeInTheDocument()
  })

  it.each<[TransactionSource, string]>([['MANUAL', 'Manual'], ['ASSISTANT_TEXT', 'Assistant'], ['VOICE', 'Voice']])('shows %s with the friendly %s source label', async (source, label) => {
    mocks.get.mockResolvedValue({ ...expense, source })
    renderDetail()
    expect(await screen.findByText(label)).toBeInTheDocument()
  })

  it('renders the authoritative multi-installment response without regenerating values', async () => {
    mocks.get.mockResolvedValue({
      ...expense, firstOccurrenceDate: '2026-08-31', installmentCount: 3,
      occurrences: [
        { sequenceNumber: 1, effectiveDate: '2026-08-10', amount: '10.01', currency: 'BRL' },
        { sequenceNumber: 2, effectiveDate: '2026-09-11', amount: '10.02', currency: 'BRL' },
        { sequenceNumber: 3, effectiveDate: '2026-10-12', amount: '10.03', currency: 'BRL' },
      ],
    })
    renderDetail()
    const installments = await screen.findByText('Schedule based on the transaction cash-flow dates.')
    const card = installments.closest<HTMLElement>('[data-slot="card"]')
    expect(card).not.toBeNull()
    expect(screen.getByText('08/31/2026')).toBeInTheDocument()
    expect(within(card!).getByText('1 / 3')).toBeInTheDocument()
    expect(within(card!).getByText('08/10/2026')).toBeInTheDocument()
    expect(within(card!).getByText(/R\$\s*10\.01/)).toBeInTheDocument()
    expect(within(card!).getByText('09/11/2026')).toBeInTheDocument()
    expect(within(card!).getByText(/R\$\s*10\.02/)).toBeInTheDocument()
    expect(within(card!).getByText('10/12/2026')).toBeInTheDocument()
    expect(within(card!).getByText(/R\$\s*10\.03/)).toBeInTheDocument()
  })

  it('keeps reimbursement receipts read-only and links to the reimbursement workspace', async () => {
    mocks.get.mockResolvedValue(reimbursement)
    renderDetail('/transactions/receipt-42')
    expect(await screen.findByText(/managed through/i)).toBeInTheDocument()
    expect(screen.getByText(/not earned income/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Owed to Me' })).toHaveAttribute('href', '/owed')
    expect(screen.queryByRole('button', { name: /edit transaction/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /delete transaction/i })).not.toBeInTheDocument()
  })

  it('keeps a reimbursement source expense read-only using backend metadata', async () => {
    mocks.get.mockResolvedValue({ ...expense, managedByReimbursement: true })
    renderDetail()

    expect(await screen.findByText(/linked to a reimbursement claim/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Owed to Me' })).toHaveAttribute('href', '/owed')
    expect(screen.queryByRole('button', { name: /edit transaction/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /delete transaction/i })).not.toBeInTheDocument()
  })

  it('does not infer reimbursement management solely from transaction kind', async () => {
    mocks.get.mockResolvedValue({
      ...reimbursement,
      managedByReimbursement: false,
    })
    renderDetail('/transactions/receipt-42')

    expect(await screen.findByRole('button', { name: 'Edit transaction' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete transaction' })).toBeInTheDocument()
    expect(screen.queryByText(/managed through/i)).not.toBeInTheDocument()
  })

  it('edits an ordinary transaction and invalidates detail, lists, and dashboard data', async () => {
    const { invalidate, user } = renderDetail()
    await user.click(await screen.findByRole('button', { name: 'Edit transaction' }))
    await user.click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(mocks.update).toHaveBeenCalledWith('transaction-42', expect.objectContaining({ description: 'Dinner with friends', amount: '30.06', eventDate: '2026-08-19', firstOccurrenceDate: '2026-08-19' })))
    expect(await screen.findByText('Transaction updated successfully.')).toBeInTheDocument()
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.transactions.detail('transaction-42') })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.transactions.lists })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.dashboard.all })
  })

  it('requires confirmation before deleting, then navigates and invalidates financial queries', async () => {
    const { invalidate, user } = renderDetail()
    await user.click(await screen.findByRole('button', { name: 'Delete transaction' }))
    expect(screen.getByText('Delete this transaction?')).toBeInTheDocument()
    expect(mocks.delete).not.toHaveBeenCalled()
    await user.click(screen.getByRole('button', { name: 'Confirm delete' }))
    await waitFor(() => expect(mocks.delete).toHaveBeenCalledWith('transaction-42'))
    expect(await screen.findByText(/Transaction list destination Transaction deleted successfully/)).toBeInTheDocument()
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.transactions.lists })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.dashboard.all })
  })

  it('shows protected feedback when the backend rejects editing with 409', async () => {
    mocks.update.mockRejectedValue(new ApiError(409, 'TRANSACTION_PROTECTED', 'internal conflict'))
    const { user } = renderDetail()
    await user.click(await screen.findByRole('button', { name: 'Edit transaction' }))
    await user.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(await screen.findByText(/protected by reimbursement data and cannot be edited/i)).toBeInTheDocument()
    expect(screen.queryByText('Transaction updated successfully.')).not.toBeInTheDocument()
  })

  it('keeps the transaction and shows protected feedback when deletion returns 409', async () => {
    mocks.delete.mockRejectedValue(new ApiError(409, 'TRANSACTION_PROTECTED', 'internal conflict'))
    const { user } = renderDetail()
    await user.click(await screen.findByRole('button', { name: 'Delete transaction' }))
    await user.click(screen.getByRole('button', { name: 'Confirm delete' }))
    expect(await screen.findByText(/protected by reimbursement data and cannot be deleted/i)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Dinner with friends' })).toBeInTheDocument()
  })

  it('uses a neutral category fallback without exposing the unknown category ID', async () => {
    mocks.categoryList.mockRejectedValue(new Error('catalog unavailable'))
    mocks.get.mockResolvedValue({ ...expense, categoryId: 'private-category-uuid' })
    renderDetail()
    expect((await screen.findAllByText('Unknown category')).length).toBeGreaterThan(0)
    expect(document.body.textContent).not.toContain('private-category-uuid')
  })
})
