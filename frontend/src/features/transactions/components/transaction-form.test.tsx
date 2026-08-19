import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { TransactionForm } from '@/features/transactions/components/transaction-form'
import type { Category } from '@/types/api'

const categories: Category[] = [
  { id: 'expense', code: 'FOOD_DINING', displayName: 'Food and dining', allowedKind: 'EXPENSE', builtIn: true },
  { id: 'income', code: 'SALARY', displayName: 'Salary', allowedKind: 'INCOME', builtIn: true },
  { id: 'any', code: 'OTHER', displayName: 'Other', allowedKind: 'ANY', builtIn: true },
  { id: 'receipt', code: 'REIMBURSEMENT', displayName: 'Reimbursement', allowedKind: 'REIMBURSEMENT_RECEIPT', builtIn: true },
]

function renderForm(onSubmit = vi.fn()) {
  render(
    <TransactionForm
      categories={categories}
      categoriesPending={false}
      categoriesError={false}
      isSubmitting={false}
      submitLabel="Create transaction"
      onSubmit={onSubmit}
      onCancel={vi.fn()}
    />,
  )
  return { onSubmit, user: userEvent.setup() }
}

function optionNames(select: HTMLElement): string[] {
  return within(select).getAllByRole('option').map((option) => option.textContent ?? '')
}

describe('TransactionForm', () => {
  it('shows EXPENSE and ANY categories only for an expense', () => {
    renderForm()

    const category = screen.getByLabelText('Category')
    expect(optionNames(category)).toEqual(['Select a category', 'Food and dining', 'Other'])
    expect(optionNames(category)).not.toContain('Reimbursement')
  })

  it('shows INCOME and ANY categories only for income', async () => {
    const { user } = renderForm()

    await user.selectOptions(screen.getByLabelText('Kind'), 'INCOME')

    const category = screen.getByLabelText('Category')
    expect(optionNames(category)).toEqual(['Select a category', 'Salary', 'Other'])
    expect(optionNames(category)).not.toContain('Reimbursement')
  })

  it('clears a selected category when the kind makes it incompatible', async () => {
    const { user } = renderForm()
    const category = screen.getByLabelText('Category')
    await user.selectOptions(category, 'expense')

    await user.selectOptions(screen.getByLabelText('Kind'), 'INCOME')

    expect(category).toHaveValue('')
  })

  it('accepts a dot decimal and submits only the exact production request fields', async () => {
    const onSubmit = vi.fn()
    const { user } = renderForm(onSubmit)
    await user.type(screen.getByLabelText('Description'), 'Coffee')
    await user.type(screen.getByLabelText('Amount'), '12.34')
    await user.selectOptions(screen.getByLabelText('Category'), 'expense')
    await user.clear(screen.getByLabelText('Event date'))
    await user.type(screen.getByLabelText('Event date'), '2026-08-19')
    await user.click(screen.getByRole('button', { name: 'Create transaction' }))

    expect(onSubmit).toHaveBeenCalledWith({
      kind: 'EXPENSE',
      description: 'Coffee',
      amount: '12.34',
      categoryId: 'expense',
      eventDate: '2026-08-19',
      installmentCount: 1,
    })
    const request = onSubmit.mock.calls[0]?.[0]
    expect(request).not.toHaveProperty('userId')
    expect(request).not.toHaveProperty('source')
    expect(request).not.toHaveProperty('currency')
  })

  it('ignores alphabetic amount input without showing a warning', async () => {
    const { user } = renderForm()
    const amount = screen.getByLabelText('Amount')

    await user.type(amount, '9a')

    expect(amount).toHaveValue('9')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('ignores a second decimal separator', async () => {
    const { user } = renderForm()
    const amount = screen.getByLabelText('Amount')
    await user.type(amount, '9.4')

    await user.type(amount, '.')

    expect(amount).toHaveValue('9.4')
  })

  it('ignores digits beyond two decimal places', async () => {
    const { user } = renderForm()
    const amount = screen.getByLabelText('Amount')
    await user.type(amount, '9.43')

    await user.type(amount, '2')

    expect(amount).toHaveValue('9.43')
  })

  it('accepts a comma decimal and normalizes it for backend submission', async () => {
    const onSubmit = vi.fn()
    const { user } = renderForm(onSubmit)
    await user.type(screen.getByLabelText('Description'), 'Coffee')
    await user.type(screen.getByLabelText('Amount'), '9,43')
    await user.selectOptions(screen.getByLabelText('Category'), 'expense')
    await user.clear(screen.getByLabelText('Event date'))
    await user.type(screen.getByLabelText('Event date'), '2026-08-19')
    await user.click(screen.getByRole('button', { name: 'Create transaction' }))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ amount: '9.43' }))
  })

  it('preserves the existing amount when a pasted candidate is invalid', async () => {
    const { user } = renderForm()
    const amount = screen.getByLabelText('Amount')
    await user.type(amount, '9.43')

    fireEvent.change(amount, { target: { value: 'R$9.43' } })

    expect(amount).toHaveValue('9.43')
  })
})
