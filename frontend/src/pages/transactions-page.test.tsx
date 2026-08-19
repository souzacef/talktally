import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api/api-client'
import { queryKeys } from '@/lib/query/query-client'
import { TransactionsPage } from '@/pages/transactions-page'
import type { Category, PageResponse, TransactionResponse } from '@/types/api'

const mocks = vi.hoisted(() => ({
  categoryList: vi.fn(),
  transactionList: vi.fn(),
  transactionCreate: vi.fn(),
  transactionUpdate: vi.fn(),
}))

vi.mock('@/features/categories/api/category-api', () => ({
  categoryApi: { list: mocks.categoryList },
}))

vi.mock('@/features/transactions/api/transaction-api', () => ({
  transactionApi: {
    list: mocks.transactionList,
    create: mocks.transactionCreate,
    update: mocks.transactionUpdate,
  },
}))

const categories: Category[] = [
  { id: 'food-id', code: 'FOOD_DINING', displayName: 'Food and dining', allowedKind: 'EXPENSE', builtIn: true },
  { id: 'salary-id', code: 'SALARY', displayName: 'Salary', allowedKind: 'INCOME', builtIn: true },
  { id: 'other-id', code: 'OTHER', displayName: 'Other', allowedKind: 'ANY', builtIn: true },
  { id: 'reimbursement-id', code: 'REIMBURSEMENT', displayName: 'Reimbursement', allowedKind: 'REIMBURSEMENT_RECEIPT', builtIn: true },
]

const expense: TransactionResponse = {
  id: 'expense-transaction',
  kind: 'EXPENSE',
  description: 'Coffee',
  amount: '12.34',
  currency: 'BRL',
  categoryId: 'food-id',
  eventDate: '2026-08-19',
  source: 'MANUAL',
  installmentCount: 1,
  occurrences: [
    { sequenceNumber: 1, effectiveDate: '2026-08-19', amount: '12.34', currency: 'BRL' },
  ],
}

const reimbursement: TransactionResponse = {
  id: 'reimbursement-transaction',
  kind: 'REIMBURSEMENT_RECEIPT',
  description: 'Reimbursement from Ana',
  amount: '60.00',
  currency: 'BRL',
  categoryId: 'reimbursement-id',
  eventDate: '2026-08-19',
  source: 'VOICE',
  installmentCount: 3,
  occurrences: [
    { sequenceNumber: 1, effectiveDate: '2026-08-19', amount: '20.00', currency: 'BRL' },
    { sequenceNumber: 2, effectiveDate: '2026-09-19', amount: '20.00', currency: 'BRL' },
    { sequenceNumber: 3, effectiveDate: '2026-10-19', amount: '20.00', currency: 'BRL' },
  ],
}

function page(items: TransactionResponse[]): PageResponse<TransactionResponse> {
  return {
    items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: items.length === 0 ? 0 : 1,
  }
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  const invalidate = vi.spyOn(client, 'invalidateQueries')
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter><TransactionsPage /></MemoryRouter>
    </QueryClientProvider>,
  )
  return { invalidate, user: userEvent.setup() }
}

describe('TransactionsPage category catalog integration', () => {
  beforeEach(() => {
    mocks.categoryList.mockResolvedValue(categories)
    mocks.transactionList.mockResolvedValue(page([expense, reimbursement]))
    mocks.transactionCreate.mockResolvedValue(expense)
    mocks.transactionUpdate.mockResolvedValue(expense)
  })

  it('renders friendly category and installment metadata while receipts remain read-only', async () => {
    renderPage()

    const expenseRow = (await screen.findByText('Coffee')).closest('li')
    const reimbursementRow = screen.getByText('Reimbursement from Ana').closest('li')
    expect(expenseRow).not.toBeNull()
    expect(reimbursementRow).not.toBeNull()
    expect(within(expenseRow!).getByText('Food and dining')).toBeInTheDocument()
    expect(within(expenseRow!).queryByText(/1 installment/)).not.toBeInTheDocument()
    expect(within(reimbursementRow!).getByText('3 installments')).toBeInTheDocument()
    expect(within(reimbursementRow!).getAllByText('Reimbursement')).toHaveLength(2)
    expect(within(reimbursementRow!).queryByText('Income')).not.toBeInTheDocument()
    expect(within(reimbursementRow!).queryByRole('button', { name: /edit/i })).not.toBeInTheDocument()
    expect(document.body.textContent).not.toContain('Category food-id')
  })

  it('uses category IDs internally when applying the friendly category filter', async () => {
    const { user } = renderPage()
    await screen.findByText('Coffee')

    await user.selectOptions(screen.getByLabelText('Category'), 'food-id')
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    await waitFor(() => expect(mocks.transactionList).toHaveBeenLastCalledWith(
      expect.objectContaining({ categoryId: 'food-id', page: 0, size: 20 }),
      expect.anything(),
    ))
  })

  it('creates with the production shape and invalidates transaction and dashboard queries', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    const { invalidate, user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const createCard = screen.getByText(/Create an ordinary expense or income/)
      .closest<HTMLElement>('[data-slot="card"]')
    expect(createCard).not.toBeNull()
    const form = within(createCard!)

    await user.type(form.getByLabelText('Description'), 'Lunch')
    await user.type(form.getByLabelText('Amount'), '45.67')
    await user.selectOptions(form.getByLabelText('Category'), 'food-id')
    await user.clear(form.getByLabelText('Event date'))
    await user.type(form.getByLabelText('Event date'), '2026-08-19')
    await user.click(form.getByRole('button', { name: 'Create transaction' }))

    await waitFor(() => expect(mocks.transactionCreate).toHaveBeenCalledWith({
      kind: 'EXPENSE',
      description: 'Lunch',
      amount: '45.67',
      categoryId: 'food-id',
      eventDate: '2026-08-19',
      installmentCount: 1,
    }))
    expect(await screen.findByText('Transaction created successfully.')).toBeInTheDocument()
    expect(screen.queryByText(/Create an ordinary expense or income/)).not.toBeInTheDocument()
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.transactions.all })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.dashboard.all })
  })

  it('edits through a full replacement request and invalidates affected queries', async () => {
    const { invalidate, user } = renderPage()
    const expenseRow = (await screen.findByText('Coffee')).closest('li')
    await user.click(within(expenseRow!).getByRole('button', { name: /edit/i }))
    const editPanel = screen.getByText('Edit transaction').parentElement
    expect(editPanel).not.toBeNull()

    await user.click(within(editPanel!).getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(mocks.transactionUpdate).toHaveBeenCalledWith(
      'expense-transaction',
      {
        kind: 'EXPENSE',
        description: 'Coffee',
        amount: '12.34',
        categoryId: 'food-id',
        eventDate: '2026-08-19',
        installmentCount: 1,
      },
    ))
    expect(await screen.findByText('Transaction updated successfully.')).toBeInTheDocument()
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.transactions.all })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.dashboard.all })
  })

  it('shows a clear protected message for a backend 409', async () => {
    mocks.transactionUpdate.mockRejectedValue(new ApiError(
      409,
      'TRANSACTION_PROTECTED',
      'transaction is linked to reimbursement data',
    ))
    const { user } = renderPage()
    const expenseRow = (await screen.findByText('Coffee')).closest('li')
    await user.click(within(expenseRow!).getByRole('button', { name: /edit/i }))
    const editPanel = screen.getByText('Edit transaction').parentElement
    await user.click(within(editPanel!).getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText(
      'This transaction is protected by reimbursement data and cannot be edited.',
    )).toBeInTheDocument()
    expect(screen.queryByText('Transaction updated successfully.')).not.toBeInTheDocument()
  })

  it('uses a neutral fallback when the category catalog fails without exposing an ID', async () => {
    mocks.categoryList.mockRejectedValue(new Error('catalog offline'))
    mocks.transactionList.mockResolvedValue(page([{ ...expense, categoryId: 'raw-uuid-fragment' }]))
    renderPage()

    expect(await screen.findByText('Unknown category')).toBeInTheDocument()
    expect(screen.getByText('Category names and category filtering are temporarily unavailable.')).toBeInTheDocument()
    expect(document.body.textContent).not.toContain('raw-uuid-fragment')
  })
})
