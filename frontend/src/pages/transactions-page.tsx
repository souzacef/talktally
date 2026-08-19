import { useState, type FormEvent } from 'react'
import { ChevronDown, Filter, Pencil, Plus, Search } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
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
import { ApiError } from '@/lib/api/api-client'
import { financialAmountStyle } from '@/lib/money/financial-display'
import { formatBrl } from '@/lib/money/format-brl'
import { queryKeys } from '@/lib/query/query-client'
import type {
  TransactionKind,
  TransactionListParams,
  TransactionRequest,
} from '@/types/api'

const initialParams: TransactionListParams = { page: 0, size: 20 }

function mutationErrorMessage(error: Error | null, action: 'create' | 'edit'): string | undefined {
  if (!error) return undefined
  if (action === 'edit' && error instanceof ApiError && error.status === 409) {
    return 'This transaction is protected by reimbursement data and cannot be edited.'
  }
  if (error instanceof ApiError) return error.message
  return action === 'create'
    ? 'The transaction could not be created.'
    : 'The transaction could not be updated.'
}

export function TransactionsPage() {
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
  const [feedback, setFeedback] = useState<string | null>(null)
  const query = useQuery({
    queryKey: queryKeys.transactions.list(params),
    queryFn: ({ signal }) => transactionApi.list(params, signal),
  })

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
      setFeedback('Transaction created successfully.')
      await invalidateFinancialQueries()
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: string; request: TransactionRequest }) =>
      transactionApi.update(id, request),
    onSuccess: async () => {
      setEditingId(null)
      setFeedback('Transaction updated successfully.')
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
          <p className="mb-1 text-xs font-bold tracking-[0.14em] text-primary uppercase">Ledger</p>
          <h1 className="font-heading text-3xl font-semibold tracking-[-0.045em] sm:text-4xl">Transactions</h1>
          <p className="mt-2 text-muted-foreground">Search and understand every server-owned financial record.</p>
        </div>
        <Button type="button" onClick={openCreate}>
          <Plus aria-hidden="true" /> Add transaction
        </Button>
      </header>

      {feedback && (
        <Alert>
          <AlertDescription>{feedback}</AlertDescription>
        </Alert>
      )}

      {showCreate && (
        <Card>
          <CardHeader>
            <CardTitle className="text-xl">Add transaction</CardTitle>
            <CardDescription>Create an ordinary expense or income using the server category catalog.</CardDescription>
          </CardHeader>
          <CardContent>
            <TransactionForm
              key="create-transaction"
              categories={categories.data ?? []}
              categoriesPending={categories.isPending}
              categoriesError={categories.isError}
              isSubmitting={createMutation.isPending}
              serverError={mutationErrorMessage(createMutation.error, 'create')}
              submitLabel="Create transaction"
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
          <CardTitle className="flex items-center gap-2 text-lg"><Filter className="size-4 text-primary" aria-hidden="true" /> Find transactions</CardTitle>
          <CardDescription>Reimbursement receipts can be viewed here, but remain server-managed.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4 md:grid-cols-2 xl:grid-cols-[minmax(14rem,1fr)_11rem_minmax(12rem,1fr)_10rem_10rem_auto]" onSubmit={applyFilters}>
            <div className="space-y-2">
              <Label htmlFor="transaction-search">Description</Label>
              <div className="relative">
                <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
                <Input id="transaction-search" className="pl-10" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search records" />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="transaction-kind">Kind</Label>
              <select
                id="transaction-kind"
                className="h-11 w-full rounded-xl border border-input bg-card px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30"
                value={kind}
                onChange={(event) => setKind(event.target.value as TransactionKind | '')}
              >
                <option value="">All kinds</option>
                <option value="INCOME">Income</option>
                <option value="EXPENSE">Expense</option>
                <option value="REIMBURSEMENT_RECEIPT">Reimbursement</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="transaction-category">Category</Label>
              <select
                id="transaction-category"
                className="h-11 w-full rounded-xl border border-input bg-card px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-60"
                value={categoryId}
                onChange={(event) => setCategoryId(event.target.value)}
                disabled={categories.isPending || categories.isError}
              >
                <option value="">
                  {categories.isPending
                    ? 'Loading categories…'
                    : categories.isError
                      ? 'Categories unavailable'
                      : 'All categories'}
                </option>
                {categories.data?.map((category) => (
                  <option key={category.id} value={category.id}>{categoryLabel(category)}</option>
                ))}
              </select>
            </div>
            <div className="space-y-2"><Label htmlFor="transaction-from">From</Label><Input id="transaction-from" type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></div>
            <div className="space-y-2"><Label htmlFor="transaction-to">To</Label><Input id="transaction-to" type="date" value={to} onChange={(event) => setTo(event.target.value)} /></div>
            <div className="flex items-end gap-2 md:col-span-2 xl:col-span-1">
              <Button type="submit" className="flex-1">Apply</Button>
              <Button type="button" variant="ghost" onClick={clearFilters}>Reset</Button>
            </div>
          </form>
          {categories.isError && (
            <p className="mt-3 text-sm text-destructive" role="alert">
              Category names and category filtering are temporarily unavailable.
            </p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-xl">Transaction ledger</CardTitle>
          <CardDescription>{query.data ? `${query.data.totalElements} records` : 'Your real API data'}</CardDescription>
        </CardHeader>
        <CardContent>
          {query.isPending && <div className="h-48 animate-pulse rounded-2xl bg-muted" aria-label="Loading transactions" />}
          {query.error && <StatePanel tone="error" title="Transactions unavailable" description="The ledger could not be loaded. Try again in a moment." />}
          {query.data?.items.length === 0 && <StatePanel title="No matching transactions" description="Try changing the filters, or use the assistant to record new activity." />}
          <ul className="divide-y">
            {query.data?.items.map((transaction) => {
              const amount = financialAmountStyle(transaction.kind)
              const protectedReceipt = transaction.kind === 'REIMBURSEMENT_RECEIPT'
              const categoryName = categories.isPending
                ? 'Loading category…'
                : categoryLabelForId(categories.data, transaction.categoryId)
              return (
                <li key={transaction.id} className="py-4 first:pt-0 last:pb-0">
                  <div className="flex items-start gap-3">
                    <KindIcon kind={transaction.kind} />
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="min-w-0 truncate font-semibold">{transaction.description}</p>
                        <span className="hidden sm:inline-flex"><KindBadge kind={transaction.kind} /></span>
                        {protectedReceipt && <ProtectedBadge />}
                      </div>
                      <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
                        <span>{transaction.eventDate}</span>
                        {transaction.installmentCount > 1 && (
                          <span>{transaction.installmentCount} installments</span>
                        )}
                        <span>{categoryName}</span>
                        <span>{transaction.source.replace('_', ' ').toLowerCase()}</span>
                      </div>
                    </div>
                    <div className="flex shrink-0 flex-col items-end gap-2">
                      <span className={`tabular-nums font-heading text-base font-semibold sm:text-lg ${amount.className}`}>
                        {amount.prefix}{formatBrl(transaction.amount)}
                      </span>
                      {!protectedReceipt && (
                        <Button
                          type="button"
                          size="sm"
                          variant="ghost"
                          onClick={() => {
                            setShowCreate(false)
                            createMutation.reset()
                            updateMutation.reset()
                            setFeedback(null)
                            setEditingId(transaction.id)
                          }}
                        >
                          <Pencil aria-hidden="true" /> Edit
                        </Button>
                      )}
                    </div>
                  </div>
                  {editingId === transaction.id && !protectedReceipt && (
                    <div className="mt-4 rounded-2xl border bg-muted/25 p-4">
                      <h2 className="mb-4 font-heading text-lg font-semibold">Edit transaction</h2>
                      <TransactionForm
                        key={`edit-${transaction.id}`}
                        categories={categories.data ?? []}
                        categoriesPending={categories.isPending}
                        categoriesError={categories.isError}
                        initialTransaction={transaction}
                        isSubmitting={updateMutation.isPending}
                        serverError={mutationErrorMessage(updateMutation.error, 'edit')}
                        submitLabel="Save changes"
                        onSubmit={(request) => updateMutation.mutate({ id: transaction.id, request })}
                        onCancel={() => {
                          updateMutation.reset()
                          setEditingId(null)
                        }}
                      />
                    </div>
                  )}
                  {transaction.occurrences.length > 1 && (
                    <details className="group ml-13 mt-3 rounded-xl bg-muted/45 px-3 py-2">
                      <summary className="flex cursor-pointer list-none items-center gap-2 text-xs font-semibold text-muted-foreground">
                        <ChevronDown className="size-3.5 transition-transform group-open:rotate-180" aria-hidden="true" />
                        Authoritative installment schedule
                      </summary>
                      <ol className="mt-3 space-y-2 border-t pt-3">
                        {transaction.occurrences.map((occurrence) => (
                          <li key={occurrence.sequenceNumber} className="flex items-center justify-between gap-3 text-xs">
                            <span>#{occurrence.sequenceNumber} · {occurrence.effectiveDate}</span>
                            <span className="tabular-nums font-semibold">{formatBrl(occurrence.amount)}</span>
                          </li>
                        ))}
                      </ol>
                    </details>
                  )}
                </li>
              )
            })}
          </ul>
          {query.data && query.data.totalPages > 1 && (
            <div className="mt-5 flex items-center justify-between border-t pt-4">
              <Button variant="outline" disabled={query.data.page === 0} onClick={() => setParams((current) => ({ ...current, page: Math.max(0, (current.page ?? 0) - 1) }))}>Previous</Button>
              <span className="text-xs text-muted-foreground">Page {query.data.page + 1} of {query.data.totalPages}</span>
              <Button variant="outline" disabled={query.data.page + 1 >= query.data.totalPages} onClick={() => setParams((current) => ({ ...current, page: (current.page ?? 0) + 1 }))}>Next</Button>
            </div>
          )}
        </CardContent>
      </Card>
    </section>
  )
}
