import { useRef, useState, type FormEvent } from 'react'
import { useLocale } from '@/app/providers/locale-provider'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  categoryLabel,
  categorySupportsKind,
  ordinaryCategoriesForKind,
} from '@/features/categories/category-presentation'
import { transactionKindLabel, transactionText } from '@/features/transactions/transaction-messages'
import type {
  Category,
  CreateReimbursementRequest,
  PersonResponse,
  TransactionRequest,
  TransactionResponse,
  UserManagedTransactionKind,
} from '@/types/api'

interface ReimbursementCreationOptions {
  people: readonly PersonResponse[]
  peoplePending: boolean
  peopleError: boolean
  isSubmitting: boolean
  serverError?: string
  personCreationPending: boolean
  personCreationError?: string
  onCreatePerson: (displayName: string) => Promise<PersonResponse>
  onSubmit: (request: CreateReimbursementRequest) => void
  onResetErrors: () => void
}

interface TransactionFormProps {
  categories: readonly Category[]
  categoriesPending: boolean
  categoriesError: boolean
  initialTransaction?: TransactionResponse
  reimbursementCreation?: ReimbursementCreationOptions
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

function amountInCents(amount: string): bigint | null {
  const normalized = normalizeAmount(amount)
  if (!/^\d+(?:\.\d{1,2})?$/.test(normalized)) return null
  const [whole, fraction = ''] = normalized.split('.')
  const cents = BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'))
  return cents > 0n ? cents : null
}

function initialValues(transaction?: TransactionResponse): FormValues {
  const eventDate = transaction?.eventDate ?? new Date().toISOString().slice(0, 10)
  const kind = transaction?.kind === 'INCOME' ? 'INCOME' : 'EXPENSE'
  return {
    kind,
    description: transaction?.description ?? '',
    amount: transaction ? String(transaction.amount) : '',
    categoryId: transaction?.categoryId ?? '',
    eventDate,
    firstOccurrenceDate: kind === 'INCOME'
      ? eventDate
      : transaction?.firstOccurrenceDate ?? eventDate,
    installmentCount: kind === 'INCOME'
      ? '1'
      : String(transaction?.installmentCount ?? 1),
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
  if (values.kind === 'EXPENSE') {
    const installments = Number(values.installmentCount)
    if (!Number.isInteger(installments) || installments < 1 || installments > 120) {
      return transactionText(locale, 'formInstallmentsInvalid')
    }
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
  reimbursementCreation,
  isSubmitting,
  serverError,
  submitLabel,
  onSubmit,
  onCancel,
}: TransactionFormProps) {
  const { locale } = useLocale()
  const [values, setValues] = useState(() => initialValues(initialTransaction))
  const firstOccurrenceDateChanged = useRef(Boolean(
    initialTransaction && initialTransaction.kind !== 'INCOME',
  ))
  const reimbursementGeneration = useRef(0)
  const [clientError, setClientError] = useState<string | null>(null)
  const [reimbursementEnabled, setReimbursementEnabled] = useState(false)
  const [personId, setPersonId] = useState('')
  const [amountOwed, setAmountOwed] = useState('')
  const [claimNote, setClaimNote] = useState('')
  const [addingPerson, setAddingPerson] = useState(false)
  const [newPersonName, setNewPersonName] = useState('')
  const [personClientError, setPersonClientError] = useState<string | null>(null)
  const compatibleCategories = ordinaryCategoriesForKind(categories, values.kind)
  const canCreateReimbursement = Boolean(reimbursementCreation && !initialTransaction)
  const reimbursementMode = canCreateReimbursement
    && values.kind === 'EXPENSE'
    && reimbursementEnabled
  const currentServerError = reimbursementMode
    ? reimbursementCreation?.serverError
    : serverError
  const currentlySubmitting = reimbursementMode
    ? reimbursementCreation?.isSubmitting ?? false
    : isSubmitting

  function clearReimbursementState() {
    reimbursementGeneration.current += 1
    setReimbursementEnabled(false)
    setPersonId('')
    setAmountOwed('')
    setClaimNote('')
    setAddingPerson(false)
    setNewPersonName('')
    setPersonClientError(null)
    reimbursementCreation?.onResetErrors()
  }

  function changeKind(nextKind: UserManagedTransactionKind) {
    firstOccurrenceDateChanged.current = false
    setValues((current) => {
      const selected = categories.find((category) => category.id === current.categoryId)
      return {
        ...current,
        kind: nextKind,
        categoryId: selected && categorySupportsKind(selected, nextKind)
          ? current.categoryId
          : '',
        firstOccurrenceDate: current.eventDate,
        installmentCount: '1',
      }
    })
    setClientError(null)
    if (nextKind !== 'EXPENSE') {
      clearReimbursementState()
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const error = validationError(values, categories, locale)
    if (error) {
      setClientError(error)
      return
    }
    setClientError(null)
    const income = values.kind === 'INCOME'
    const transactionRequest: TransactionRequest = {
      kind: values.kind,
      description: values.description.trim(),
      amount: normalizeAmount(values.amount),
      categoryId: values.categoryId,
      eventDate: values.eventDate,
      firstOccurrenceDate: income
        ? values.eventDate
        : values.firstOccurrenceDate || values.eventDate,
      installmentCount: income ? 1 : Number(values.installmentCount),
    }
    if (!reimbursementMode || !reimbursementCreation) {
      onSubmit(transactionRequest)
      return
    }
    if (!personId) {
      setClientError(transactionText(locale, 'formPersonRequired'))
      return
    }
    const expenseCents = amountInCents(values.amount)
    const owedCents = amountOwed ? amountInCents(amountOwed) : null
    if (amountOwed && owedCents === null) {
      setClientError(transactionText(locale, 'formAmountOwedInvalid'))
      return
    }
    if (owedCents !== null && expenseCents !== null && owedCents > expenseCents) {
      setClientError(transactionText(locale, 'formAmountOwedExceedsExpense'))
      return
    }
    reimbursementCreation.onSubmit({
      description: transactionRequest.description,
      amount: transactionRequest.amount,
      categoryId: transactionRequest.categoryId,
      eventDate: transactionRequest.eventDate,
      firstOccurrenceDate: transactionRequest.firstOccurrenceDate,
      installmentCount: transactionRequest.installmentCount,
      personId,
      amountOwed: amountOwed ? normalizeAmount(amountOwed) : null,
      note: claimNote.trim() || null,
    })
  }

  async function createPerson() {
    if (!reimbursementCreation) return
    const displayName = newPersonName.trim()
    if (!displayName) {
      setPersonClientError(transactionText(locale, 'formPersonNameRequired'))
      return
    }
    setPersonClientError(null)
    reimbursementCreation.onResetErrors()
    const generation = reimbursementGeneration.current
    try {
      const person = await reimbursementCreation.onCreatePerson(displayName)
      if (generation !== reimbursementGeneration.current) return
      setPersonId(person.id)
      setNewPersonName('')
      setAddingPerson(false)
    }
    catch {
      // The mutation error remains visible beside the independent person action.
    }
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
        <div className={values.kind === 'EXPENSE' ? 'space-y-2 xl:col-start-1' : 'space-y-2'}>
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
                firstOccurrenceDate: current.kind !== 'INCOME' && firstOccurrenceDateChanged.current
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
        {values.kind === 'EXPENSE' && (
          <>
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
          </>
        )}
      </div>

      {canCreateReimbursement && values.kind === 'EXPENSE' && (
        <section
          className="rounded-2xl border border-reimbursement/20 bg-reimbursement-soft/35 p-4"
          aria-labelledby="reimbursement-option-label"
        >
          <div className="flex items-start gap-3">
            <input
              id="reimbursement-option"
              type="checkbox"
              className="mt-1 size-4 shrink-0 accent-reimbursement"
              checked={reimbursementEnabled}
              aria-describedby="reimbursement-option-help"
              onChange={(event) => {
                setClientError(null)
                if (event.target.checked) {
                  reimbursementCreation?.onResetErrors()
                  setReimbursementEnabled(true)
                }
                else {
                  clearReimbursementState()
                }
              }}
            />
            <div>
              <Label id="reimbursement-option-label" htmlFor="reimbursement-option">
                {transactionText(locale, 'someoneOwesMe')}
              </Label>
              <p id="reimbursement-option-help" className="mt-1 text-xs text-muted-foreground">
                {transactionText(locale, 'reimbursementHelper')}
              </p>
            </div>
          </div>

          {reimbursementMode && reimbursementCreation && (
            <div className="mt-4 grid gap-4 border-t border-reimbursement/15 pt-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="reimbursement-person">{transactionText(locale, 'person')}</Label>
                <select
                  id="reimbursement-person"
                  className="h-11 w-full rounded-xl border border-input bg-card px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-60"
                  value={personId}
                  onChange={(event) => {
                    setPersonId(event.target.value)
                    setClientError(null)
                  }}
                  disabled={reimbursementCreation.peoplePending}
                  required
                >
                  <option value="">
                    {reimbursementCreation.peoplePending
                      ? transactionText(locale, 'loadingPeople')
                      : transactionText(locale, 'selectPerson')}
                  </option>
                  {reimbursementCreation.people.map((person) => (
                    <option key={person.id} value={person.id}>{person.displayName}</option>
                  ))}
                </select>
                {reimbursementCreation.peopleError && (
                  <p className="text-xs text-destructive" role="status">
                    {transactionText(locale, 'peopleUnavailableForReimbursement')}
                  </p>
                )}
                {!reimbursementCreation.peoplePending
                  && !reimbursementCreation.peopleError
                  && reimbursementCreation.people.length === 0 && (
                  <p className="text-xs text-muted-foreground">
                    {transactionText(locale, 'noPeopleForReimbursement')}
                  </p>
                )}
                {!addingPerson ? (
                  <Button type="button" size="sm" variant="ghost" onClick={() => {
                    setAddingPerson(true)
                    setPersonClientError(null)
                    reimbursementCreation.onResetErrors()
                  }}>
                    {transactionText(locale, 'addNewPerson')}
                  </Button>
                ) : (
                  <div className="space-y-2 rounded-xl border bg-card p-3">
                    <Label htmlFor="reimbursement-new-person">{transactionText(locale, 'personName')}</Label>
                    <Input
                      id="reimbursement-new-person"
                      value={newPersonName}
                      maxLength={120}
                      onChange={(event) => {
                        setNewPersonName(event.target.value)
                        setPersonClientError(null)
                      }}
                    />
                    {(personClientError || reimbursementCreation.personCreationError) && (
                      <p className="text-xs text-destructive" role="alert">
                        {personClientError ?? reimbursementCreation.personCreationError}
                      </p>
                    )}
                    <div className="flex flex-wrap justify-end gap-2">
                      <Button type="button" size="sm" variant="ghost" onClick={() => {
                        setAddingPerson(false)
                        setNewPersonName('')
                        setPersonClientError(null)
                        reimbursementCreation.onResetErrors()
                      }}>
                        {transactionText(locale, 'cancel')}
                      </Button>
                      <Button type="button" size="sm" disabled={reimbursementCreation.personCreationPending} onClick={() => void createPerson()}>
                        {reimbursementCreation.personCreationPending
                          ? transactionText(locale, 'creatingPerson')
                          : transactionText(locale, 'createPerson')}
                      </Button>
                    </div>
                  </div>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="reimbursement-amount-owed">{transactionText(locale, 'amountOwed')}</Label>
                <Input
                  id="reimbursement-amount-owed"
                  type="text"
                  inputMode="decimal"
                  placeholder={locale === 'pt-BR' ? '0,00' : '0.00'}
                  value={amountOwed}
                  aria-describedby="reimbursement-amount-owed-help"
                  onChange={(event) => {
                    const candidate = event.target.value
                    if (AMOUNT_INPUT_PATTERN.test(candidate)) {
                      setAmountOwed(candidate)
                      setClientError(null)
                    }
                  }}
                />
                <p id="reimbursement-amount-owed-help" className="text-xs text-muted-foreground">
                  {transactionText(locale, 'amountOwedHelp')}
                </p>
              </div>
              <div className="space-y-2 md:col-span-2">
                <Label htmlFor="reimbursement-note">{transactionText(locale, 'reimbursementNote')}</Label>
                <Textarea
                  id="reimbursement-note"
                  className="min-h-20 resize-y"
                  value={claimNote}
                  maxLength={500}
                  onChange={(event) => setClaimNote(event.target.value)}
                />
              </div>
            </div>
          )}
        </section>
      )}

      {(clientError || currentServerError) && (
        <Alert variant="destructive">
          <AlertDescription>{clientError ?? currentServerError}</AlertDescription>
        </Alert>
      )}
      {categoriesError && !clientError && !currentServerError && (
        <Alert variant="destructive">
          <AlertDescription>{transactionText(locale, 'categoriesLoadFailed')}</AlertDescription>
        </Alert>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" onClick={onCancel}>{transactionText(locale, 'cancel')}</Button>
        <Button type="submit" disabled={currentlySubmitting || categoriesPending || categoriesError}>
          {currentlySubmitting ? transactionText(locale, 'saving') : submitLabel}
        </Button>
      </div>
    </form>
  )
}
