import { useState, type FormEvent } from 'react'
import { ChevronDown, Filter, Pencil, Plus, Search } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useLocation } from 'react-router-dom'
import { useLocale } from '@/app/providers/locale-provider'
import { StatePanel } from '@/components/feedback/state-panel'
import {
  KindBadge,
  KindIcon,
  ProtectedBadge,
} from '@/components/finance/financial-visuals'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  categoryLabel,
  categoryLabelForId,
} from '@/features/categories/category-presentation'
import { useCategories } from '@/features/categories/hooks/use-categories'
import { transactionApi } from '@/features/transactions/api/transaction-api'
import { TransactionForm } from '@/features/transactions/components/transaction-form'
import {
  transactionKindLabel,
  transactionSourceLabel,
  transactionText,
} from '@/features/transactions/transaction-messages'
import { ApiError } from '@/lib/api/api-client'
import { financialAmountStyle } from '@/lib/money/financial-display'
import { queryKeys } from '@/lib/query/query-client'
import type {
  TransactionKind,
  TransactionListParams,
  TransactionRequest,
} from '@/types/api'

const initialParams: TransactionListParams = { page: 0, size: 20 }

export function TransactionsPage() {
  const { locale, formatMoney, formatDate } = useLocale()
  const location = useLocation()
  const queryClient = useQueryClient()
  const categories = useCategories()
  const [search, setSearch] = useState('')
  const [kind, setKind] = useState<TransactionKind | ''>('')
  const [categoryId, setCategoryId] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [params, setParams] = useState<TransactionListParams>(initialParams)
  const [showCreate, setShowCreate] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<string | null>(() => {
    const state = location.state as { feedback?: unknown } | null
    return typeof state?.feedback === 'string' ? state.feedback : null
  })
  const query = useQuery({
    queryKey: queryKeys.transactions.list(params),
    queryFn: ({ signal }) => transactionApi.list(params, signal),
  })

  function mutationErrorMessage(error: Error | null, action: 'create' | 'edit'): string | undefined {
    if (!error) return undefined
    if (action === 'edit' && error instanceof ApiError && error.status === 409) {
      return transactionText(locale, 'protectedEdit')
    }
    if (error instanceof ApiError) return error.message
    return transactionText(locale, action === 'create' ? 'createFailed' : 'updateFailed')
  }

  async function invalidateFinancialQueries() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions.all }),
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all }),
    ])
  }

  const createMutation = useMutation({
    mutationFn: (request: TransactionRequest) => transactionApi.create(request),
    onSuccess: async () => {
      setShowCreate(false)
      setFeedback(transactionText(locale, 'created'))
      await invalidateFinancialQueries()
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: string; request: TransactionRequest }) =>
      transactionApi.update(id, request),
    onSuccess: async () => {
      setEditingId(null)
      setFeedback(transactionText(locale, 'updated'))
      await invalidateFinancialQueries()
    },
  })

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setParams({
      page: 0,
      size: 20,
      search: search.trim() || undefined,
      kind: kind || undefined,
      categoryId: categoryId || undefined,
      from: from || undefined,
      to: to || undefined,
    })
  }

  function clearFilters() {
    setSearch('')
    setKind('')
    setCategoryId('')
    setFrom('')
    setTo('')
    setParams(initialParams)
  }

  function openCreate() {
    setEditingId(null)
    updateMutation.reset()
    createMutation.reset()
    setFeedback(null)
    setShowCreate(true)
  }

  return (
    <section className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="mb-1 text-xs font-bold tracking-[0.14em] text-primary uppercase">{transactionText(locale, 'ledgerEyebrow')}</p>
          <h1 className="font-heading text-3xl font-semibold tracking-[-0.045em] sm:text-4xl">{transactionText(locale, 'title')}</h1>
          <p className="mt-2 text-muted-foreground">{transactionText(locale, 'subtitle')}</p>
        </div>
        <Button type="button" onClick={openCreate}>
          <Plus aria-hidden="true" /> {transactionText(locale, 'addTransaction')}
        </Button>
      </header>

      {feedback && <Alert><AlertDescription>{feedback}</AlertDescription></Alert>}

      {showCreate && (
        <Card>
          <CardHeader>
            <CardTitle className="text-xl">{transactionText(locale, 'addTransaction')}</CardTitle>
            <CardDescription>{transactionText(locale, 'addDescription')}</CardDescription>
          </CardHeader>
          <CardContent>
            <TransactionForm
              key="create-transaction"
              categories={categories.data ?? []}
              categoriesPending={categories.isPending}
              categoriesError={categories.isError}
              isSubmitting={createMutation.isPending}
              serverError={mutationErrorMessage(createMutation.error, 'create')}
              submitLabel={transactionText(locale, 'createTransaction')}
              onSubmit={(request) => createMutation.mutate(request)}
              onCancel={() => {
                createMutation.reset()
                setShowCreate(false)
              }}
            />
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg"><Filter className="size-4 text-primary" aria-hidden="true" /> {transactionText(locale, 'findTransactions')}</CardTitle>
          <CardDescription>{transactionText(locale, 'findDescription')}</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4 md:grid-cols-2 xl:grid-cols-[minmax(14rem,1fr)_11rem_minmax(12rem,1fr)_10rem_10rem_auto]" onSubmit={applyFilters}>
            <div className="space-y-2">
              <Label htmlFor="transaction-search">{transactionText(locale, 'description')}</Label>
              <div className="relative">
                <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
                <Input id="transaction-search" className="pl-10" value={search} onChange={(event) => setSearch(event.target.value)} placeholder={transactionText(locale, 'searchRecords')} />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="transaction-kind">{transactionText(locale, 'kind')}</Label>
              <select id="transaction-kind" className="h-11 w-full rounded-xl border border-input bg-card px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30" value={kind} onChange={(event) => setKind(event.target.value as TransactionKind | '')}>
                <option value="">{transactionText(locale, 'allKinds')}</option>
                <option value="INCOME">{transactionKindLabel('INCOME', locale)}</option>
                <option value="EXPENSE">{transactionKindLabel('EXPENSE', locale)}</option>
                <option value="REIMBURSEMENT_RECEIPT">{transactionKindLabel('REIMBURSEMENT_RECEIPT', locale)}</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="transaction-category">{transactionText(locale, 'category')}</Label>
              <select id="transaction-category" className="h-11 w-full rounded-xl border border-input bg-card px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-60" value={categoryId} onChange={(event) => setCategoryId(event.target.value)} disabled={categories.isPending || categories.isError}>
                <option value="">
                  {categories.isPending ? transactionText(locale, 'loadingCategories') : categories.isError ? transactionText(locale, 'categoriesUnavailable') : transactionText(locale, 'allCategories')}
                </option>
                {categories.data?.map((category) => <option key={category.id} value={category.id}>{categoryLabel(category, locale)}</option>)}
              </select>
            </div>
            <div className="space-y-2"><Label htmlFor="transaction-from">{transactionText(locale, 'from')}</Label><Input id="transaction-from" type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></div>
            <div className="space-y-2"><Label htmlFor="transaction-to">{transactionText(locale, 'to')}</Label><Input id="transaction-to" type="date" value={to} onChange={(event) => setTo(event.target.value)} /></div>
            <div className="flex items-end gap-2 md:col-span-2 xl:col-span-1">
              <Button type="submit" className="flex-1">{transactionText(locale, 'apply')}</Button>
              <Button type="button" variant="ghost" onClick={clearFilters}>{transactionText(locale, 'reset')}</Button>
            </div>
          </form>
          {categories.isError && <p className="mt-3 text-sm text-destructive" role="alert">{transactionText(locale, 'categoryFilteringUnavailable')}</p>}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-xl">{transactionText(locale, 'transactionLedger')}</CardTitle>
          <CardDescription>{query.data ? transactionText(locale, 'records', { count: query.data.totalElements }) : transactionText(locale, 'realApiData')}</CardDescription>
        </CardHeader>
        <CardContent>
          {query.isPending && <div className="h-48 animate-pulse rounded-2xl bg-muted" aria-label={transactionText(locale, 'loadingTransactions')} />}
          {query.error && <StatePanel tone="error" title={transactionText(locale, 'transactionsUnavailable')} description={transactionText(locale, 'transactionsUnavailableDescription')} />}
          {query.data?.items.length === 0 && <StatePanel title={transactionText(locale, 'noMatchingTransactions')} description={transactionText(locale, 'noMatchingTransactionsDescription')} />}
          <ul className="divide-y">
            {query.data?.items.map((transaction) => {
              const amount = financialAmountStyle(transaction.kind)
              const protectedReceipt = transaction.kind === 'REIMBURSEMENT_RECEIPT'
              const categoryName = categories.isPending ? transactionText(locale, 'loadingCategory') : categoryLabelForId(categories.data, transaction.categoryId, locale)
              return (
                <li key={transaction.id} className="py-4 first:pt-0 last:pb-0">
                  <div className="group/ledger-row relative flex items-start gap-3 rounded-xl transition-colors hover:bg-muted/45">
                    <Link to={`/transactions/${transaction.id}`} aria-label={transactionText(locale, 'viewTransaction', { description: transaction.description })} className="absolute inset-0 rounded-xl outline-none focus-visible:ring-3 focus-visible:ring-ring/30" />
                    <KindIcon kind={transaction.kind} />
                    <div className="pointer-events-none min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="min-w-0 truncate font-semibold">{transaction.description}</p>
                        <span className="hidden sm:inline-flex"><KindBadge kind={transaction.kind} label={transactionKindLabel(transaction.kind, locale)} /></span>
                        {protectedReceipt && <ProtectedBadge label={transactionText(locale, 'protected')} />}
                      </div>
                      <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
                        <span>{formatDate(transaction.eventDate)}</span>
                        {transaction.installmentCount > 1 && <span>{transactionText(locale, 'installments', { count: transaction.installmentCount })}</span>}
                        <span>{categoryName}</span>
                        <span>{transactionSourceLabel(transaction.source, locale)}</span>
                      </div>
                    </div>
                    <div className="pointer-events-none flex shrink-0 flex-col items-end gap-2">
                      <span className={`tabular-nums font-heading text-base font-semibold sm:text-lg ${amount.className}`}>{amount.prefix}{formatMoney(transaction.amount)}</span>
                      {!protectedReceipt && (
                        <Button type="button" size="sm" variant="ghost" className="pointer-events-auto relative z-10" onClick={(event) => {
                          event.stopPropagation()
                          setShowCreate(false)
                          createMutation.reset()
                          updateMutation.reset()
                          setFeedback(null)
                          setEditingId(transaction.id)
                        }}>
                          <Pencil aria-hidden="true" /> {transactionText(locale, 'edit')}
                        </Button>
                      )}
                    </div>
                  </div>
                  {editingId === transaction.id && !protectedReceipt && (
                    <div className="mt-4 rounded-2xl border bg-muted/25 p-4">
                      <h2 className="mb-4 font-heading text-lg font-semibold">{transactionText(locale, 'editTransaction')}</h2>
                      <TransactionForm key={`edit-${transaction.id}`} categories={categories.data ?? []} categoriesPending={categories.isPending} categoriesError={categories.isError} initialTransaction={transaction} isSubmitting={updateMutation.isPending} serverError={mutationErrorMessage(updateMutation.error, 'edit')} submitLabel={transactionText(locale, 'saveChanges')} onSubmit={(request) => updateMutation.mutate({ id: transaction.id, request })} onCancel={() => { updateMutation.reset(); setEditingId(null) }} />
                    </div>
                  )}
                  {transaction.occurrences.length > 1 && (
                    <details className="group ml-13 mt-3 rounded-xl bg-muted/45 px-3 py-2">
                      <summary className="flex cursor-pointer list-none items-center gap-2 text-xs font-semibold text-muted-foreground">
                        <ChevronDown className="size-3.5 transition-transform group-open:rotate-180" aria-hidden="true" /> {transactionText(locale, 'authoritativeSchedule')}
                      </summary>
                      <ol className="mt-3 space-y-2 border-t pt-3">
                        {transaction.occurrences.map((occurrence) => <li key={occurrence.sequenceNumber} className="flex items-center justify-between gap-3 text-xs"><span>#{occurrence.sequenceNumber} · {formatDate(occurrence.effectiveDate)}</span><span className="tabular-nums font-semibold">{formatMoney(occurrence.amount)}</span></li>)}
                      </ol>
                    </details>
                  )}
                </li>
              )
            })}
          </ul>
          {query.data && query.data.totalPages > 1 && (
            <div className="mt-5 flex items-center justify-between border-t pt-4">
              <Button variant="outline" disabled={query.data.page === 0} onClick={() => setParams((current) => ({ ...current, page: Math.max(0, (current.page ?? 0) - 1) }))}>{transactionText(locale, 'previous')}</Button>
              <span className="text-xs text-muted-foreground">{transactionText(locale, 'pageOf', { page: query.data.page + 1, total: query.data.totalPages })}</span>
              <Button variant="outline" disabled={query.data.page + 1 >= query.data.totalPages} onClick={() => setParams((current) => ({ ...current, page: (current.page ?? 0) + 1 }))}>{transactionText(locale, 'next')}</Button>
            </div>
          )}
        </CardContent>
      </Card>
    </section>
  )
}
