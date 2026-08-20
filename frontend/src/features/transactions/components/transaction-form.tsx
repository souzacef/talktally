import { useRef, useState, type FormEvent } from 'react'
import { useLocale } from '@/app/providers/locale-provider'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  categoryLabel,
  categorySupportsKind,
  ordinaryCategoriesForKind,
} from '@/features/categories/category-presentation'
import { transactionKindLabel, transactionText } from '@/features/transactions/transaction-messages'
import type {
  Category,
  TransactionRequest,
  TransactionResponse,
  UserManagedTransactionKind,
} from '@/types/api'

interface TransactionFormProps {
  categories: readonly Category[]
  categoriesPending: boolean
  categoriesError: boolean
  initialTransaction?: TransactionResponse
  isSubmitting: boolean
  serverError?: string
  submitLabel: string
  onSubmit: (request: TransactionRequest) => void
  onCancel: () => void
}

interface FormValues {
  kind: UserManagedTransactionKind
  description: string
  amount: string
  categoryId: string
  eventDate: string
  firstOccurrenceDate: string
  installmentCount: string
}

const AMOUNT_INPUT_PATTERN = /^(?:\d+(?:[.,]\d{0,2})?)?$/

function normalizeAmount(amount: string): string {
  return amount.replace(',', '.')
}

function initialValues(transaction?: TransactionResponse): FormValues {
  const eventDate = transaction?.eventDate ?? new Date().toISOString().slice(0, 10)
  return {
    kind: transaction?.kind === 'INCOME' ? 'INCOME' : 'EXPENSE',
    description: transaction?.description ?? '',
    amount: transaction ? String(transaction.amount) : '',
    categoryId: transaction?.categoryId ?? '',
    eventDate,
    firstOccurrenceDate: transaction?.firstOccurrenceDate ?? eventDate,
    installmentCount: String(transaction?.installmentCount ?? 1),
  }
}

function validationError(
  values: FormValues,
  categories: readonly Category[],
  locale: 'en-US' | 'pt-BR',
): string | null {
  if (!values.description.trim()) return transactionText(locale, 'formDescriptionRequired')
  const normalizedAmount = normalizeAmount(values.amount)
  if (!/^\d+(?:\.\d{1,2})?$/.test(normalizedAmount) || Number(normalizedAmount) <= 0) {
    return transactionText(locale, 'formAmountInvalid')
  }
  if (!values.eventDate) return transactionText(locale, 'formEventDateRequired')
  const installments = Number(values.installmentCount)
  if (!Number.isInteger(installments) || installments < 1 || installments > 120) {
    return transactionText(locale, 'formInstallmentsInvalid')
  }
  const category = categories.find((candidate) => candidate.id === values.categoryId)
  if (!category || !categorySupportsKind(category, values.kind)) {
    return transactionText(locale, 'formCategoryInvalid')
  }
  return null
}

export function TransactionForm({
  categories,
  categoriesPending,
  categoriesError,
  initialTransaction,
  isSubmitting,
  serverError,
  submitLabel,
  onSubmit,
  onCancel,
}: TransactionFormProps) {
  const { locale } = useLocale()
  const [values, setValues] = useState(() => initialValues(initialTransaction))
  const firstOccurrenceDateChanged = useRef(Boolean(initialTransaction))
  const [clientError, setClientError] = useState<string | null>(null)
  const compatibleCategories = ordinaryCategoriesForKind(categories, values.kind)

  function changeKind(nextKind: UserManagedTransactionKind) {
    const selected = categories.find((category) => category.id === values.categoryId)
    setValues((current) => ({
      ...current,
      kind: nextKind,
      categoryId: selected && categorySupportsKind(selected, nextKind)
        ? current.categoryId
        : '',
    }))
    setClientError(null)
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const error = validationError(values, categories, locale)
    if (error) {
      setClientError(error)
      return
    }
    setClientError(null)
    onSubmit({
      kind: values.kind,
      description: values.description.trim(),
      amount: normalizeAmount(values.amount),
      categoryId: values.categoryId,
      eventDate: values.eventDate,
      firstOccurrenceDate: values.firstOccurrenceDate || values.eventDate,
      installmentCount: Number(values.installmentCount),
    })
  }

  return (
    <form className="space-y-4" onSubmit={submit} noValidate>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <div className="space-y-2">
          <Label htmlFor="managed-transaction-kind">{transactionText(locale, 'kind')}</Label>
          <select
            id="managed-transaction-kind"
            className="h-11 w-full rounded-xl border border-input bg-card px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30"
            value={values.kind}
            onChange={(event) => changeKind(event.target.value as UserManagedTransactionKind)}
          >
            <option value="EXPENSE">{transactionKindLabel('EXPENSE', locale)}</option>
            <option value="INCOME">{transactionKindLabel('INCOME', locale)}</option>
          </select>
        </div>
        <div className="space-y-2 md:col-span-1 xl:col-span-2">
          <Label htmlFor="managed-transaction-description">{transactionText(locale, 'description')}</Label>
          <Input
            id="managed-transaction-description"
            value={values.description}
            onChange={(event) => setValues((current) => ({ ...current, description: event.target.value }))}
            maxLength={500}
            required
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="managed-transaction-amount">{transactionText(locale, 'amount')}</Label>
          <Input
            id="managed-transaction-amount"
            type="text"
            inputMode="decimal"
            placeholder={locale === 'pt-BR' ? '0,00' : '0.00'}
            value={values.amount}
            onChange={(event) => {
              const candidate = event.target.value
              if (AMOUNT_INPUT_PATTERN.test(candidate)) {
                setValues((current) => ({ ...current, amount: candidate }))
              }
            }}
            required
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="managed-transaction-category">{transactionText(locale, 'category')}</Label>
          <select
            id="managed-transaction-category"
            className="h-11 w-full rounded-xl border border-input bg-card px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-60"
            value={values.categoryId}
            onChange={(event) => setValues((current) => ({ ...current, categoryId: event.target.value }))}
            disabled={categoriesPending || categoriesError}
            required
          >
            <option value="">
              {categoriesPending
                ? transactionText(locale, 'loadingCategories')
                : categoriesError
                  ? transactionText(locale, 'categoriesUnavailable')
                  : transactionText(locale, 'selectCategory')}
            </option>
            {compatibleCategories.map((category) => (
              <option key={category.id} value={category.id}>{categoryLabel(category, locale)}</option>
            ))}
          </select>
        </div>
        <div className="space-y-2 xl:col-start-1">
          <Label htmlFor="managed-transaction-date">{transactionText(locale, 'eventDate')}</Label>
          <Input
            id="managed-transaction-date"
            type="date"
            value={values.eventDate}
            aria-describedby="managed-transaction-date-help"
            onChange={(event) => {
              const eventDate = event.target.value
              setValues((current) => ({
                ...current,
                eventDate,
                firstOccurrenceDate: firstOccurrenceDateChanged.current
                  ? current.firstOccurrenceDate
                  : eventDate,
              }))
            }}
            required
          />
          <p id="managed-transaction-date-help" className="text-xs text-muted-foreground">
            {transactionText(locale, 'eventDateHelp')}
          </p>
        </div>
        <div className="space-y-2">
          <Label htmlFor="managed-transaction-first-occurrence-date">{transactionText(locale, 'firstCashFlowDate')}</Label>
          <Input
            id="managed-transaction-first-occurrence-date"
            type="date"
            value={values.firstOccurrenceDate}
            aria-describedby="managed-transaction-first-occurrence-date-help"
            onChange={(event) => {
              firstOccurrenceDateChanged.current = true
              setValues((current) => ({
                ...current,
                firstOccurrenceDate: event.target.value,
              }))
            }}
            required
          />
          <p
            id="managed-transaction-first-occurrence-date-help"
            className="text-xs text-muted-foreground"
          >
            {transactionText(locale, 'firstCashFlowDateHelp')}
          </p>
        </div>
        <div className="space-y-2">
          <Label htmlFor="managed-transaction-installments">{transactionText(locale, 'installmentsTitle')}</Label>
          <Input
            id="managed-transaction-installments"
            type="number"
            min={1}
            max={120}
            step={1}
            value={values.installmentCount}
            onChange={(event) => setValues((current) => ({ ...current, installmentCount: event.target.value }))}
            required
          />
        </div>
      </div>

      {(clientError || serverError) && (
        <Alert variant="destructive">
          <AlertDescription>{clientError ?? serverError}</AlertDescription>
        </Alert>
      )}
      {categoriesError && !clientError && !serverError && (
        <Alert variant="destructive">
          <AlertDescription>{transactionText(locale, 'categoriesLoadFailed')}</AlertDescription>
        </Alert>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" onClick={onCancel}>{transactionText(locale, 'cancel')}</Button>
        <Button type="submit" disabled={isSubmitting || categoriesPending || categoriesError}>
          {isSubmitting ? transactionText(locale, 'saving') : submitLabel}
        </Button>
      </div>
    </form>
  )
}
