import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LOCALE_KEY, LocaleProvider } from '@/app/providers/locale-provider'
import { ApiError } from '@/lib/api/api-client'
import { queryKeys } from '@/lib/query/query-client'
import { TransactionsPage } from '@/pages/transactions-page'
import type { Category, PageResponse, TransactionResponse } from '@/types/api'

const mocks = vi.hoisted(() => ({
  categoryList: vi.fn(),
  transactionList: vi.fn(),
  transactionCreate: vi.fn(),
  transactionUpdate: vi.fn(),
  peopleList: vi.fn(),
  peopleCreate: vi.fn(),
  reimbursementCreate: vi.fn(),
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

vi.mock('@/features/reimbursements/api/people-api', () => ({
  peopleApi: {
    list: mocks.peopleList,
    create: mocks.peopleCreate,
  },
}))

vi.mock('@/features/reimbursements/api/reimbursement-api', () => ({
  reimbursementApi: { create: mocks.reimbursementCreate },
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
  firstOccurrenceDate: '2026-08-19',
  source: 'MANUAL',
  installmentCount: 1,
  managedByReimbursement: false,
  createdAt: '2026-08-22T17:35:00Z',
  updatedAt: '2026-08-22T17:35:00Z',
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
  firstOccurrenceDate: '2026-08-19',
  source: 'VOICE',
  installmentCount: 3,
  managedByReimbursement: true,
  createdAt: '2026-08-22T17:35:00Z',
  updatedAt: '2026-08-22T17:35:00Z',
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

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location">{location.pathname}</output>
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
    <LocaleProvider>
      <QueryClientProvider client={client}>
        <MemoryRouter><TransactionsPage /><LocationProbe /></MemoryRouter>
      </QueryClientProvider>
    </LocaleProvider>,
  )
  return { invalidate, user: userEvent.setup() }
}

describe('TransactionsPage category catalog integration', () => {
  beforeEach(() => {
    window.localStorage.setItem(LOCALE_KEY, 'en-US')
    mocks.categoryList.mockResolvedValue(categories)
    mocks.transactionList.mockResolvedValue(page([expense, reimbursement]))
    mocks.transactionCreate.mockResolvedValue(expense)
    mocks.transactionUpdate.mockResolvedValue(expense)
    mocks.peopleList.mockResolvedValue([{ id: 'ana-id', displayName: 'Ana Silva' }])
    mocks.peopleCreate.mockResolvedValue({ id: 'rose-id', displayName: 'Rose' })
    mocks.reimbursementCreate.mockResolvedValue({})
  })

  it('renders friendly category and installment metadata while receipts remain read-only', async () => {
    renderPage()

    const expenseRow = (await screen.findByText('Coffee')).closest('li')
    const reimbursementRow = screen.getByText('Reimbursement from Ana').closest('li')
    expect(expenseRow).not.toBeNull()
    expect(reimbursementRow).not.toBeNull()
    expect(within(expenseRow!).getByText('Food and dining')).toBeInTheDocument()
    expect(within(expenseRow!).getByText(/^Recorded /)).toBeInTheDocument()
    expect(within(expenseRow!).getByText('08/19/2026')).toBeInTheDocument()
    expect(within(expenseRow!).queryByText(/1 installment/)).not.toBeInTheDocument()
    expect(within(reimbursementRow!).getByText('3 installments')).toBeInTheDocument()
    expect(within(reimbursementRow!).getAllByText('Reimbursement')).toHaveLength(2)
    expect(within(reimbursementRow!).queryByText('Income')).not.toBeInTheDocument()
    expect(within(reimbursementRow!).queryByRole('button', { name: /edit/i })).not.toBeInTheDocument()
    expect(document.body.textContent).not.toContain('Category food-id')
  })

  it('hides ordinary edit for a source expense managed by reimbursement', async () => {
    mocks.transactionList.mockResolvedValue(page([
      { ...expense, id: 'managed-expense', description: 'Dinner at Outback', managedByReimbursement: true },
    ]))
    renderPage()

    const managedRow = (await screen.findByText('Dinner at Outback')).closest('li')
    expect(managedRow).not.toBeNull()
    expect(within(managedRow!).getByText('Protected')).toBeInTheDocument()
    expect(within(managedRow!).queryByRole('button', { name: /edit/i })).not.toBeInTheDocument()
  })

  it('renders transaction labels, categories, dates, and BRL amounts in pt-BR', async () => {
    window.localStorage.setItem(LOCALE_KEY, 'pt-BR')
    renderPage()

    expect(screen.getByRole('heading', { name: 'Transações' })).toBeInTheDocument()
    const expenseRow = (await screen.findByText('Coffee')).closest('li')
    expect(expenseRow).not.toBeNull()
    expect(within(expenseRow!).getByText('Alimentação')).toBeInTheDocument()
    expect(within(expenseRow!).getByText('19/08/2026')).toBeInTheDocument()
    expect(within(expenseRow!).getByText(/^Registrado em /)).toBeInTheDocument()
    expect(within(expenseRow!).getByText('Despesa')).toBeInTheDocument()
    expect(within(expenseRow!).getByText(/R\$\s*12,34/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Aplicar' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Limpar' })).toBeInTheDocument()
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

  it('keeps both filter actions in a separate full-width row', () => {
    renderPage()

    const apply = screen.getByRole('button', { name: 'Apply' })
    const actionArea = apply.parentElement
    const form = apply.closest('form')
    expect(form).toHaveClass('xl:grid-cols-5')
    expect(form?.className).not.toContain('2xl:grid-cols-[')
    expect(actionArea).toHaveClass('col-span-full', 'flex-wrap', 'justify-end')
    expect(apply).toHaveClass('shrink-0')
    expect(screen.getByRole('button', { name: 'Reset' })).toHaveClass('shrink-0')
  })

  it('creates with the production shape and invalidates transaction and dashboard queries', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    const { invalidate, user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const createCard = screen.getByText(/Record an expense or income/)
      .closest<HTMLElement>('[data-slot="card"]')
    expect(createCard).not.toBeNull()
    const form = within(createCard!)

    expect(form.getByRole('checkbox', { name: 'Track reimbursement for this expense' })).toHaveAccessibleDescription(
      'Track who should reimburse you and how much, without changing the expense.',
    )
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
      firstOccurrenceDate: '2026-08-19',
      installmentCount: 1,
    }))
    expect(await screen.findByText('Transaction created successfully.')).toBeInTheDocument()
    expect(screen.queryByText(/Record an expense or income/)).not.toBeInTheDocument()
    expect(mocks.reimbursementCreate).not.toHaveBeenCalled()
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.transactions.all })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.dashboard.all })
  })

  it('routes an existing-person reimbursable expense only through the atomic endpoint', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    const { invalidate, user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const form = within(screen.getByText(/Record an expense or income/).closest<HTMLElement>('[data-slot="card"]')!)

    await user.type(form.getByLabelText('Description'), 'Dinner at Outback')
    await user.type(form.getByLabelText('Amount'), '200.00')
    await user.selectOptions(form.getByLabelText('Category'), 'food-id')
    await user.clear(form.getByLabelText('Event date'))
    await user.type(form.getByLabelText('Event date'), '2026-08-19')
    await user.clear(form.getByLabelText('First cash-flow date'))
    await user.type(form.getByLabelText('First cash-flow date'), '2026-09-10')
    await user.clear(form.getByLabelText('Installments'))
    await user.type(form.getByLabelText('Installments'), '3')
    await user.click(form.getByRole('checkbox', { name: 'Track reimbursement for this expense' }))
    await user.selectOptions(await form.findByLabelText('Person'), 'ana-id')
    await user.type(form.getByLabelText('Optional reimbursement note'), 'Rose owes half')
    await user.click(form.getByRole('button', { name: 'Create transaction' }))

    await waitFor(() => expect(mocks.reimbursementCreate).toHaveBeenCalledWith({
      description: 'Dinner at Outback',
      amount: '200.00',
      categoryId: 'food-id',
      eventDate: '2026-08-19',
      firstOccurrenceDate: '2026-09-10',
      installmentCount: 3,
      personId: 'ana-id',
      amountOwed: null,
      note: 'Rose owes half',
    }))
    expect(mocks.transactionCreate).not.toHaveBeenCalled()
    expect(await screen.findByText('Reimbursable expense created.')).toBeInTheDocument()
    expect(screen.queryByText(/Record an expense or income/)).not.toBeInTheDocument()
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.transactions.all })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.reimbursements.all })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.people.all })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.dashboard.all })
  })

  it('preserves a partial owed amount without replacing the source expense amount', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    const { user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const form = within(screen.getByText(/Record an expense or income/).closest<HTMLElement>('[data-slot="card"]')!)

    await user.type(form.getByLabelText('Description'), 'Shared dinner')
    await user.type(form.getByLabelText('Amount'), '200,00')
    await user.selectOptions(form.getByLabelText('Category'), 'food-id')
    await user.click(form.getByRole('checkbox', { name: 'Track reimbursement for this expense' }))
    await user.selectOptions(await form.findByLabelText('Person'), 'ana-id')
    await user.type(form.getByLabelText('Amount owed'), '100,00')
    await user.click(form.getByRole('button', { name: 'Create transaction' }))

    await waitFor(() => expect(mocks.reimbursementCreate).toHaveBeenCalledWith(
      expect.objectContaining({
        description: 'Shared dinner',
        amount: '200.00',
        amountOwed: '100.00',
        personId: 'ana-id',
      }),
    ))
    expect(mocks.transactionCreate).not.toHaveBeenCalled()
  })

  it('clears reimbursement intent when switching away from expense', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    const { user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const form = within(screen.getByText(/Record an expense or income/).closest<HTMLElement>('[data-slot="card"]')!)

    const checkbox = form.getByRole('checkbox', { name: 'Track reimbursement for this expense' })
    await user.click(checkbox)
    await user.selectOptions(await form.findByLabelText('Person'), 'ana-id')
    await user.type(form.getByLabelText('Amount owed'), '10.00')
    await user.selectOptions(form.getByLabelText('Kind'), 'INCOME')
    expect(form.queryByRole('checkbox', { name: 'Track reimbursement for this expense' })).not.toBeInTheDocument()
    expect(form.queryByLabelText('Amount owed')).not.toBeInTheDocument()

    await user.selectOptions(form.getByLabelText('Kind'), 'EXPENSE')
    expect(form.getByRole('checkbox', { name: 'Track reimbursement for this expense' })).not.toBeChecked()
    expect(form.queryByLabelText('Amount owed')).not.toBeInTheDocument()
  })

  it('does not restore cleared reimbursement state when person creation finishes late', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    let resolvePerson: (person: { id: string, displayName: string }) => void = () => undefined
    mocks.peopleCreate.mockImplementation(() => new Promise((resolve) => {
      resolvePerson = resolve
    }))
    mocks.peopleList
      .mockResolvedValueOnce([{ id: 'ana-id', displayName: 'Ana Silva' }])
      .mockResolvedValue([{ id: 'ana-id', displayName: 'Ana Silva' }, { id: 'rose-id', displayName: 'Rose' }])
    const { user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const form = within(screen.getByText(/Record an expense or income/).closest<HTMLElement>('[data-slot="card"]')!)

    await user.click(form.getByRole('checkbox', { name: 'Track reimbursement for this expense' }))
    await user.click(form.getByRole('button', { name: 'Add new person' }))
    await user.type(form.getByLabelText('Person name'), 'Rose')
    await user.click(form.getByRole('button', { name: 'Add person' }))
    await waitFor(() => expect(mocks.peopleCreate).toHaveBeenCalledWith({ displayName: 'Rose' }))

    await user.selectOptions(form.getByLabelText('Kind'), 'INCOME')
    resolvePerson({ id: 'rose-id', displayName: 'Rose' })
    await waitFor(() => expect(mocks.peopleCreate).toHaveReturned())
    await user.selectOptions(form.getByLabelText('Kind'), 'EXPENSE')

    const checkbox = form.getByRole('checkbox', { name: 'Track reimbursement for this expense' })
    expect(checkbox).not.toBeChecked()
    await user.click(checkbox)
    expect(await form.findByLabelText('Person')).toHaveValue('')
    expect(mocks.transactionCreate).not.toHaveBeenCalled()
    expect(mocks.reimbursementCreate).not.toHaveBeenCalled()
  })

  it('creates a person explicitly, selects the server response, then submits the expense separately', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    mocks.peopleList
      .mockResolvedValueOnce([{ id: 'ana-id', displayName: 'Ana Silva' }])
      .mockResolvedValue([{ id: 'ana-id', displayName: 'Ana Silva' }, { id: 'rose-id', displayName: 'Rose' }])
    const { invalidate, user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const form = within(screen.getByText(/Record an expense or income/).closest<HTMLElement>('[data-slot="card"]')!)

    await user.type(form.getByLabelText('Description'), 'Shared lunch')
    await user.type(form.getByLabelText('Amount'), '80.00')
    await user.selectOptions(form.getByLabelText('Category'), 'food-id')
    await user.click(form.getByRole('checkbox', { name: 'Track reimbursement for this expense' }))
    await user.click(form.getByRole('button', { name: 'Add new person' }))
    await user.type(form.getByLabelText('Person name'), 'Rose')
    await user.click(form.getByRole('button', { name: 'Add person' }))

    await waitFor(() => expect(mocks.peopleCreate).toHaveBeenCalledWith({ displayName: 'Rose' }))
    expect(await form.findByLabelText('Person')).toHaveValue('rose-id')
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.people.all })
    expect(mocks.transactionCreate).not.toHaveBeenCalled()
    expect(mocks.reimbursementCreate).not.toHaveBeenCalled()

    await user.click(form.getByRole('button', { name: 'Create transaction' }))
    await waitFor(() => expect(mocks.reimbursementCreate).toHaveBeenCalledWith(
      expect.objectContaining({ personId: 'rose-id', amount: '80.00', amountOwed: null }),
    ))
  })

  it('keeps financial values and does not submit when explicit person creation fails', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    mocks.peopleCreate.mockRejectedValue(new ApiError(
      409,
      'PERSON_ALREADY_EXISTS',
      'internal duplicate detail',
    ))
    const { user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const form = within(screen.getByText(/Record an expense or income/).closest<HTMLElement>('[data-slot="card"]')!)

    await user.type(form.getByLabelText('Description'), 'Dinner')
    await user.type(form.getByLabelText('Amount'), '75.00')
    await user.selectOptions(form.getByLabelText('Category'), 'food-id')
    await user.click(form.getByRole('checkbox', { name: 'Track reimbursement for this expense' }))
    await user.click(form.getByRole('button', { name: 'Add new person' }))
    await user.type(form.getByLabelText('Person name'), 'Ana Silva')
    await user.click(form.getByRole('button', { name: 'Add person' }))

    expect(await form.findByText('A person with that name already exists.')).toBeInTheDocument()
    expect(form.queryByText('internal duplicate detail')).not.toBeInTheDocument()
    expect(form.getByLabelText('Description')).toHaveValue('Dinner')
    expect(form.getByLabelText('Amount')).toHaveValue('75.00')
    expect(form.getByLabelText('Person name')).toHaveValue('Ana Silva')
    expect(mocks.transactionCreate).not.toHaveBeenCalled()
    expect(mocks.reimbursementCreate).not.toHaveBeenCalled()
  })

  it('validates a selected person and deterministic owed amount before submission', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    const { user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const form = within(screen.getByText(/Record an expense or income/).closest<HTMLElement>('[data-slot="card"]')!)

    await user.type(form.getByLabelText('Description'), 'Shared expense')
    await user.type(form.getByLabelText('Amount'), '200.00')
    await user.selectOptions(form.getByLabelText('Category'), 'food-id')
    await user.click(form.getByRole('checkbox', { name: 'Track reimbursement for this expense' }))
    await user.click(form.getByRole('button', { name: 'Create transaction' }))
    expect(await form.findByText('Select the person who owes you.')).toBeInTheDocument()

    await user.selectOptions(form.getByLabelText('Person'), 'ana-id')
    await user.type(form.getByLabelText('Amount owed'), '0')
    await user.click(form.getByRole('button', { name: 'Create transaction' }))
    expect(await form.findByText(/Amount owed must be positive/)).toBeInTheDocument()

    await user.clear(form.getByLabelText('Amount owed'))
    await user.type(form.getByLabelText('Amount owed'), '200.01')
    await user.click(form.getByRole('button', { name: 'Create transaction' }))
    expect(await form.findByText('Amount owed cannot be greater than the expense amount.')).toBeInTheDocument()
    expect(mocks.transactionCreate).not.toHaveBeenCalled()
    expect(mocks.reimbursementCreate).not.toHaveBeenCalled()
  })

  it('keeps all form data and never falls back to ordinary creation after reimbursement failure', async () => {
    mocks.transactionList.mockResolvedValue(page([]))
    mocks.reimbursementCreate.mockRejectedValue(new ApiError(
      400,
      'INVALID_REIMBURSEMENT_REQUEST',
      'internal validation detail',
    ))
    const { user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Add transaction' }))
    const form = within(screen.getByText(/Record an expense or income/).closest<HTMLElement>('[data-slot="card"]')!)

    await user.type(form.getByLabelText('Description'), 'Shared taxi')
    await user.type(form.getByLabelText('Amount'), '90.00')
    await user.selectOptions(form.getByLabelText('Category'), 'food-id')
    await user.click(form.getByRole('checkbox', { name: 'Track reimbursement for this expense' }))
    await user.selectOptions(await form.findByLabelText('Person'), 'ana-id')
    await user.type(form.getByLabelText('Amount owed'), '45.00')
    await user.type(form.getByLabelText('Optional reimbursement note'), 'Airport ride')
    await user.click(form.getByRole('button', { name: 'Create transaction' }))

    expect(await form.findByText(/reimbursable expense could not be created/i)).toBeInTheDocument()
    expect(form.queryByText('internal validation detail')).not.toBeInTheDocument()
    expect(form.getByLabelText('Description')).toHaveValue('Shared taxi')
    expect(form.getByLabelText('Amount')).toHaveValue('90.00')
    expect(form.getByLabelText('Amount owed')).toHaveValue('45.00')
    expect(form.getByLabelText('Optional reimbursement note')).toHaveValue('Airport ride')
    expect(mocks.reimbursementCreate).toHaveBeenCalledTimes(1)
    expect(mocks.transactionCreate).not.toHaveBeenCalled()
  })

  it('renders natural pt-BR reimbursement copy and localized success feedback', async () => {
    window.localStorage.setItem(LOCALE_KEY, 'pt-BR')
    mocks.transactionList.mockResolvedValue(page([]))
    const { user } = renderPage()
    await user.click(screen.getByRole('button', { name: 'Adicionar transação' }))
    const form = within(screen.getByText(/Registre uma despesa ou renda/).closest<HTMLElement>('[data-slot="card"]')!)

    const checkbox = form.getByRole('checkbox', { name: 'Registrar reembolso desta despesa' })
    expect(checkbox).toHaveAccessibleDescription('Acompanhe quem deve reembolsar você e o valor, sem alterar a despesa.')
    await user.type(form.getByLabelText('Descrição'), 'Almoço compartilhado')
    await user.type(form.getByLabelText('Valor'), '60,00')
    await user.selectOptions(form.getByLabelText('Categoria'), 'food-id')
    await user.click(checkbox)
    expect(await form.findByLabelText('Pessoa')).toBeInTheDocument()
    expect(form.getByLabelText('Valor devido')).toHaveAccessibleDescription(
      'Deixe em branco para usar o valor total da despesa.',
    )
    expect(form.getByLabelText('Observação opcional do reembolso')).toBeInTheDocument()
    expect(form.getByRole('button', { name: 'Adicionar nova pessoa' })).toBeInTheDocument()
    await user.selectOptions(form.getByLabelText('Pessoa'), 'ana-id')
    await user.click(form.getByRole('button', { name: 'Criar transação' }))

    expect(await screen.findByText('Despesa reembolsável criada.')).toBeInTheDocument()
  })

  it('edits through a full replacement request and invalidates affected queries', async () => {
    const { invalidate, user } = renderPage()
    const expenseRow = (await screen.findByText('Coffee')).closest('li')
    await user.click(within(expenseRow!).getByRole('button', { name: /edit/i }))
    const editPanel = screen.getByText('Edit transaction').parentElement
    expect(editPanel).not.toBeNull()
    expect(within(editPanel!).queryByRole('checkbox', { name: 'Track reimbursement for this expense' })).not.toBeInTheDocument()

    await user.click(within(editPanel!).getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(mocks.transactionUpdate).toHaveBeenCalledWith(
      'expense-transaction',
      {
        kind: 'EXPENSE',
        description: 'Coffee',
        amount: '12.34',
        categoryId: 'food-id',
        eventDate: '2026-08-19',
        firstOccurrenceDate: '2026-08-19',
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

  it('opens transaction detail from a ledger row', async () => {
    const { user } = renderPage()
    const rowLink = await screen.findByRole('link', { name: 'View transaction Coffee' })

    await user.click(rowLink)
    expect(screen.getByTestId('location')).toHaveTextContent('/transactions/expense-transaction')
  })

  it('opens transaction detail from a keyboard-focused ledger row', async () => {
    const { user } = renderPage()
    const rowLink = await screen.findByRole('link', { name: 'View transaction Coffee' })

    rowLink.focus()
    await user.keyboard('{Enter}')
    expect(screen.getByTestId('location')).toHaveTextContent('/transactions/expense-transaction')
  })

  it('does not navigate the ledger row when its Edit action is used', async () => {
    const { user } = renderPage()
    const expenseRow = (await screen.findByText('Coffee')).closest('li')

    await user.click(within(expenseRow!).getByRole('button', { name: /edit/i }))
    expect(screen.getByText('Edit transaction')).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/')
  })
})
