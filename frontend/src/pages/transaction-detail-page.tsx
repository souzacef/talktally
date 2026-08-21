import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowLeft,
  CalendarDays,
  Layers3,
  Mic2,
  Pencil,
  Tag,
  Trash2,
} from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useLocale } from '@/app/providers/locale-provider'
import { StatePanel } from '@/components/feedback/state-panel'
import { KindBadge, KindIcon } from '@/components/finance/financial-visuals'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { categoryLabelForId } from '@/features/categories/category-presentation'
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
import type { TransactionRequest } from '@/types/api'

export function TransactionDetailPage() {
  const { locale, formatMoney, formatDate } = useLocale()
  const { transactionId = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const categories = useCategories()
  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [feedback, setFeedback] = useState<string | null>(null)
  const detailQuery = useQuery({
    queryKey: queryKeys.transactions.detail(transactionId),
    queryFn: ({ signal }) => transactionApi.get(transactionId, signal),
    enabled: Boolean(transactionId),
  })

  function mutationErrorMessage(error: Error | null, action: 'edited' | 'deleted'): string | undefined {
    if (!error) return undefined
    if (error instanceof ApiError && error.status === 409) {
      return transactionText(locale, action === 'edited' ? 'protectedEditContext' : 'protectedDeleteContext')
    }
    if (error instanceof ApiError) return error.message
    return transactionText(locale, action === 'edited' ? 'updateFailed' : 'deleteFailed')
  }

  async function invalidateAfterEdit() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions.detail(transactionId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions.lists }),
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all }),
    ])
  }

  async function invalidateAfterDelete() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions.detail(transactionId), refetchType: 'none' }),
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions.lists }),
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all }),
    ])
  }

  const updateMutation = useMutation({
    mutationFn: (request: TransactionRequest) => transactionApi.update(transactionId, request),
    onSuccess: async () => {
      setEditing(false)
      setFeedback(transactionText(locale, 'updated'))
      await invalidateAfterEdit()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => transactionApi.delete(transactionId),
    onSuccess: async () => {
      await invalidateAfterDelete()
      navigate('/transactions', { replace: true, state: { feedback: transactionText(locale, 'deleted') } })
    },
  })

  if (detailQuery.isPending) {
    return (
      <section className="space-y-6" aria-label={transactionText(locale, 'loadingDetail')}>
        <div className="h-9 w-40 animate-pulse rounded-xl bg-muted" />
        <div className="h-72 animate-pulse rounded-2xl bg-muted" />
      </section>
    )
  }

  if (detailQuery.error instanceof ApiError && detailQuery.error.status === 404) {
    return (
      <section className="space-y-5">
        <Link to="/transactions" className="inline-flex items-center gap-2 text-sm font-semibold text-primary hover:underline">
          <ArrowLeft className="size-4" aria-hidden="true" /> {transactionText(locale, 'backToTransactions')}
        </Link>
        <StatePanel title={transactionText(locale, 'notFound')} description={transactionText(locale, 'notFoundDescription')} />
      </section>
    )
  }

  if (detailQuery.error || !detailQuery.data) {
    return (
      <section className="space-y-5">
        <Link to="/transactions" className="inline-flex items-center gap-2 text-sm font-semibold text-primary hover:underline">
          <ArrowLeft className="size-4" aria-hidden="true" /> {transactionText(locale, 'backToTransactions')}
        </Link>
        <StatePanel tone="error" title={transactionText(locale, 'unavailable')} description={transactionText(locale, 'unavailableDescription')} />
        <Button type="button" variant="outline" onClick={() => void detailQuery.refetch()}>{transactionText(locale, 'tryAgain')}</Button>
      </section>
    )
  }

  const transaction = detailQuery.data
  const amount = financialAmountStyle(transaction.kind)
  const isReceipt = transaction.kind === 'REIMBURSEMENT_RECEIPT'
  const managedByReimbursement = transaction.managedByReimbursement
  const categoryName = categories.isPending ? transactionText(locale, 'loadingCategory') : categoryLabelForId(categories.data, transaction.categoryId, locale)

  return (
    <section className="space-y-6">
      <Link to="/transactions" className="inline-flex items-center gap-2 text-sm font-semibold text-primary hover:underline">
        <ArrowLeft className="size-4" aria-hidden="true" /> {transactionText(locale, 'backToTransactions')}
      </Link>

      {feedback && <Alert><AlertDescription>{feedback}</AlertDescription></Alert>}

      <Card className={isReceipt ? 'border-reimbursement/30 bg-[linear-gradient(145deg,var(--card),var(--reimbursement-soft))]' : undefined}>
        <CardContent className="grid gap-6 py-2 md:grid-cols-[auto_minmax(0,1fr)_auto] md:items-center">
          <KindIcon kind={transaction.kind} className="size-12" />
          <div className="min-w-0">
            <div className="mb-2 flex flex-wrap items-center gap-2">
              <KindBadge kind={transaction.kind} label={transactionKindLabel(transaction.kind, locale)} />
              {transaction.installmentCount > 1 && <span className="rounded-full bg-muted px-2.5 py-1 text-[0.68rem] font-semibold text-muted-foreground">{transactionText(locale, 'installments', { count: transaction.installmentCount })}</span>}
            </div>
            <h1 className="font-heading text-2xl font-semibold tracking-[-0.04em] sm:text-3xl">{transaction.description}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{categoryName}</p>
          </div>
          <p className={`tabular-nums font-heading text-3xl font-semibold tracking-[-0.04em] md:text-right ${amount.className}`}>
            {amount.prefix}{formatMoney(transaction.amount)}
          </p>
        </CardContent>
      </Card>

      {categories.isError && <p className="text-sm text-muted-foreground" role="status">{transactionText(locale, 'categoryDetailsUnavailable')}</p>}

      {managedByReimbursement && (
        <Alert className="border-reimbursement/25 bg-reimbursement-soft/50">
          <AlertDescription>
            {transactionText(locale, isReceipt ? 'receiptManagedPrefix' : 'sourceExpenseManagedPrefix')}
            <Link to="/owed" className="font-semibold text-primary underline-offset-4 hover:underline">
              {transactionText(locale, 'receiptManagedLink')}
            </Link>
            {transactionText(locale, isReceipt ? 'receiptManagedSuffix' : 'sourceExpenseManagedSuffix')}
          </AlertDescription>
        </Alert>
      )}

      <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1.35fr)_minmax(18rem,0.65fr)]">
        <Card>
          <CardHeader>
            <CardTitle className="text-xl">{transactionText(locale, 'details')}</CardTitle>
            <CardDescription>{transactionText(locale, 'detailsDescription')}</CardDescription>
          </CardHeader>
          <CardContent>
            <dl className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-xl bg-muted/45 p-4">
                <dt className="flex items-center gap-2 text-xs font-semibold text-muted-foreground"><Tag className="size-4" aria-hidden="true" /> {transactionText(locale, 'category')}</dt>
                <dd className="mt-2 font-semibold">{categoryName}</dd>
              </div>
              <div className="rounded-xl bg-muted/45 p-4">
                <dt className="flex items-center gap-2 text-xs font-semibold text-muted-foreground"><CalendarDays className="size-4" aria-hidden="true" /> {transactionText(locale, 'eventDate')}</dt>
                <dd className="mt-2 font-semibold">{formatDate(transaction.eventDate)}</dd>
              </div>
              {transaction.firstOccurrenceDate !== transaction.eventDate && (
                <div className="rounded-xl bg-muted/45 p-4">
                  <dt className="flex items-center gap-2 text-xs font-semibold text-muted-foreground"><CalendarDays className="size-4" aria-hidden="true" /> {transactionText(locale, 'firstCashFlowDate')}</dt>
                  <dd className="mt-2 font-semibold">{formatDate(transaction.firstOccurrenceDate)}</dd>
                </div>
              )}
              <div className="rounded-xl bg-muted/45 p-4">
                <dt className="flex items-center gap-2 text-xs font-semibold text-muted-foreground"><Mic2 className="size-4" aria-hidden="true" /> {transactionText(locale, 'recordedVia')}</dt>
                <dd className="mt-2 font-semibold">{transactionSourceLabel(transaction.source, locale)}</dd>
              </div>
              <div className="rounded-xl bg-muted/45 p-4">
                <dt className="flex items-center gap-2 text-xs font-semibold text-muted-foreground"><Layers3 className="size-4" aria-hidden="true" /> {transactionText(locale, 'kind')}</dt>
                <dd className="mt-2 font-semibold"><KindBadge kind={transaction.kind} label={transactionKindLabel(transaction.kind, locale)} /></dd>
              </div>
            </dl>
          </CardContent>
        </Card>

        {!managedByReimbursement && (
          <Card>
            <CardHeader>
              <CardTitle className="text-xl">{transactionText(locale, 'actions')}</CardTitle>
              <CardDescription>{transactionText(locale, 'actionsDescription')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <Button type="button" variant="outline" className="w-full" onClick={() => {
                setFeedback(null)
                setConfirmingDelete(false)
                deleteMutation.reset()
                updateMutation.reset()
                setEditing(true)
              }}><Pencil aria-hidden="true" /> {transactionText(locale, 'editTransaction')}</Button>
              {!confirmingDelete ? (
                <Button type="button" variant="destructive" className="w-full" onClick={() => {
                  setEditing(false)
                  updateMutation.reset()
                  setConfirmingDelete(true)
                }}><Trash2 aria-hidden="true" /> {transactionText(locale, 'deleteTransaction')}</Button>
              ) : (
                <div className="space-y-3 rounded-xl border border-destructive/25 bg-destructive/5 p-4">
                  <p className="font-semibold text-destructive">{transactionText(locale, 'deletePrompt')}</p>
                  <p className="text-xs text-muted-foreground">{transactionText(locale, 'deleteWarning')}</p>
                  <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                    <Button type="button" variant="ghost" onClick={() => setConfirmingDelete(false)}>{transactionText(locale, 'cancel')}</Button>
                    <Button type="button" variant="destructive" disabled={deleteMutation.isPending} onClick={() => deleteMutation.mutate()}>
                      {deleteMutation.isPending ? transactionText(locale, 'deleting') : transactionText(locale, 'confirmDelete')}
                    </Button>
                  </div>
                </div>
              )}
              {mutationErrorMessage(deleteMutation.error, 'deleted') && <Alert variant="destructive"><AlertDescription>{mutationErrorMessage(deleteMutation.error, 'deleted')}</AlertDescription></Alert>}
            </CardContent>
          </Card>
        )}
      </div>

      {editing && !managedByReimbursement && (
        <Card>
          <CardHeader>
            <CardTitle className="text-xl">{transactionText(locale, 'editTransaction')}</CardTitle>
            <CardDescription>{transactionText(locale, 'editDescription')}</CardDescription>
          </CardHeader>
          <CardContent>
            <TransactionForm key={`detail-edit-${transaction.id}`} categories={categories.data ?? []} categoriesPending={categories.isPending} categoriesError={categories.isError} initialTransaction={transaction} isSubmitting={updateMutation.isPending} serverError={mutationErrorMessage(updateMutation.error, 'edited')} submitLabel={transactionText(locale, 'saveChanges')} onSubmit={(request) => updateMutation.mutate(request)} onCancel={() => { updateMutation.reset(); setEditing(false) }} />
          </CardContent>
        </Card>
      )}

      {transaction.occurrences.length > 1 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-xl">{transactionText(locale, 'installmentsTitle')}</CardTitle>
            <CardDescription>{transactionText(locale, 'installmentsDescription')}</CardDescription>
          </CardHeader>
          <CardContent>
            <ol className="divide-y">
              {transaction.occurrences.map((occurrence) => (
                <li key={occurrence.sequenceNumber} className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0">
                  <div>
                    <p className="font-semibold">{occurrence.sequenceNumber} / {transaction.installmentCount}</p>
                    <p className="text-sm text-muted-foreground">{formatDate(occurrence.effectiveDate)}</p>
                  </div>
                  <span className="tabular-nums font-heading font-semibold">{formatMoney(occurrence.amount)}</span>
                </li>
              ))}
            </ol>
          </CardContent>
        </Card>
      )}
    </section>
  )
}
