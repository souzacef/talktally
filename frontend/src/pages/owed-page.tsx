import { useEffect, useState, type FormEvent } from 'react'
import { HandCoins, History, Users } from 'lucide-react'
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { useLocale } from '@/app/providers/locale-provider'
import { ClaimStatusBadge } from '@/components/finance/financial-visuals'
import { StatePanel } from '@/components/feedback/state-panel'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { categoryLabelForId } from '@/features/categories/category-presentation'
import { useCategories } from '@/features/categories/hooks/use-categories'
import { useFinancialSummary } from '@/features/dashboard/hooks/use-dashboard'
import { peopleApi } from '@/features/reimbursements/api/people-api'
import { reimbursementApi } from '@/features/reimbursements/api/reimbursement-api'
import {
  reimbursementCountText,
  reimbursementStatusLabel,
  reimbursementText,
} from '@/features/reimbursements/reimbursement-messages'
import { currentMonthRange } from '@/lib/dates/reporting-periods'
import { queryKeys } from '@/lib/query/query-client'
import type { ReimbursementPaymentRequest } from '@/types/api'

const CLAIMS_PAGE_SIZE = 20

function initials(displayName: string): string {
  return displayName.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || '?'
}

export function OwedPage() {
  const queryClient = useQueryClient()
  const { locale, formatDate, formatMoney } = useLocale()
  const text = (
    key: Parameters<typeof reimbursementText>[1],
    params?: Parameters<typeof reimbursementText>[2],
  ) => reimbursementText(locale, key, params)
  const period = currentMonthRange()
  const [claimsPage, setClaimsPage] = useState(0)
  const claimsParams = { page: claimsPage, size: CLAIMS_PAGE_SIZE }
  const categories = useCategories()
  const summary = useFinancialSummary(period.from, period.to)
  const people = useQuery({
    queryKey: queryKeys.people.all,
    queryFn: ({ signal }) => peopleApi.list(signal),
  })
  const claims = useQuery({
    queryKey: queryKeys.reimbursements.list(claimsParams),
    queryFn: ({ signal }) => reimbursementApi.list(claimsParams, signal),
  })
  const personSummaries = useQueries({
    queries: (people.data ?? []).map((person) => ({
      queryKey: [...queryKeys.people.detail(person.id), 'reimbursement-summary'],
      queryFn: ({ signal }: { signal: AbortSignal }) => peopleApi.reimbursementSummary(person.id, signal),
    })),
  })
  const [activeClaimId, setActiveClaimId] = useState<string | null>(null)
  const [amount, setAmount] = useState('')
  const [receivedDate, setReceivedDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [note, setNote] = useState('')

  useEffect(() => {
    if (!claims.data || claimsPage === 0) return
    if (claims.data.totalPages === 0) {
      setClaimsPage(0)
      return
    }
    if (claims.data.items.length === 0 && claimsPage >= claims.data.totalPages) {
      setClaimsPage(claims.data.totalPages - 1)
    }
  }, [claims.data, claimsPage])

  const payment = useMutation({
    mutationFn: ({ claimId, request }: { claimId: string; request: ReimbursementPaymentRequest }) =>
      reimbursementApi.recordPayment(claimId, request),
    onSuccess: async () => {
      setActiveClaimId(null)
      setAmount('')
      setNote('')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.reimbursements.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.people.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all }),
      ])
    },
  })

  function recordPayment(event: FormEvent<HTMLFormElement>, claimId: string) {
    event.preventDefault()
    payment.mutate({
      claimId,
      request: {
        amount: amount.replace(',', '.'),
        receivedDate,
        note: note.trim() || null,
      },
    })
  }

  return (
    <section className="space-y-6">
      <header>
        <p className="mb-1 text-xs font-bold tracking-[0.14em] text-reimbursement uppercase">{text('eyebrow')}</p>
        <h1 className="font-heading text-3xl font-semibold tracking-[-0.045em] sm:text-4xl">{text('title')}</h1>
        <p className="mt-2 text-muted-foreground">{text('subtitle')}</p>
      </header>

      <Card className="border-primary bg-primary text-primary-foreground shadow-[var(--shadow-lift)]">
        <CardContent className="grid gap-6 py-2 sm:grid-cols-[1.5fr_1fr_1fr] sm:items-end">
          <div>
            <div className="mb-6 flex items-center gap-2 text-sm font-semibold"><HandCoins className="size-5" aria-hidden="true" /> {text('totalOutstanding')}</div>
            <p className="tabular-nums font-heading text-4xl font-semibold tracking-[-0.05em] sm:text-5xl">
              {summary.data ? formatMoney(summary.data.owedToMe.outstanding) : '—'}
            </p>
          </div>
          <div className="border-t border-white/15 pt-4 sm:border-l sm:border-t-0 sm:pl-6 sm:pt-0">
            <p className="font-heading text-3xl font-semibold">{summary.data?.owedToMe.openClaims ?? '—'}</p>
            <p className="mt-1 text-sm opacity-70">{summary.data ? reimbursementCountText(locale, 'openClaims', summary.data.owedToMe.openClaims) : text('openClaims')}</p>
          </div>
          <div className="border-t border-white/15 pt-4 sm:border-l sm:border-t-0 sm:pl-6 sm:pt-0">
            <p className="font-heading text-3xl font-semibold">{people.data?.length ?? '—'}</p>
            <p className="mt-1 text-sm opacity-70">{people.data ? reimbursementCountText(locale, 'people', people.data.length) : text('people')}</p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2 text-xl"><Users className="size-5 text-reimbursement" aria-hidden="true" /> {text('people')}</CardTitle><CardDescription>{text('peopleDescription')}</CardDescription></CardHeader>
        <CardContent>
          {people.isPending && <div className="h-32 animate-pulse rounded-2xl bg-muted" aria-label={text('loadingPeople')} />}
          {people.error && <StatePanel tone="error" title={text('peopleUnavailable')} description={text('peopleUnavailableDescription')} />}
          {people.data?.length === 0 && <StatePanel title={text('noPeople')} description={text('noPeopleDescription')} />}
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {people.data?.map((person, index) => {
              const personSummary = personSummaries[index]
              return (
                <article key={person.id} className="rounded-2xl border bg-muted/25 p-4">
                  <div className="flex items-center gap-3">
                    <span className="grid size-11 place-items-center rounded-full bg-reimbursement-soft font-heading font-semibold text-reimbursement">{initials(person.displayName)}</span>
                    <div className="min-w-0"><h2 className="truncate font-heading font-semibold">{person.displayName}</h2><p className="text-xs text-muted-foreground">{personSummary.data ? reimbursementCountText(locale, 'openClaimsCount', personSummary.data.openClaimCount) : '—'}</p></div>
                  </div>
                  <p className="tabular-nums mt-5 font-heading text-xl font-semibold text-reimbursement">
                    {personSummary.data ? formatMoney(personSummary.data.totalOutstanding) : '—'}
                  </p>
                  <p className="text-xs text-muted-foreground">{text('outstanding')}</p>
                </article>
              )
            })}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="text-xl">{text('claims')}</CardTitle><CardDescription>{text('claimsDescription')}</CardDescription></CardHeader>
        <CardContent>
          {claims.isPending && <div className="h-48 animate-pulse rounded-2xl bg-muted" aria-label={text('loadingClaims')} />}
          {claims.error && <StatePanel tone="error" title={text('claimsUnavailable')} description={text('claimsUnavailableDescription')} />}
          {claims.data?.items.length === 0 && <StatePanel title={text('noClaims')} description={text('noClaimsDescription')} />}
          <div className="space-y-4">
            {claims.data?.items.map((claim) => (
              <article key={claim.id} className="rounded-2xl border bg-card p-4 shadow-xs sm:p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="grid size-10 shrink-0 place-items-center rounded-full bg-reimbursement-soft font-heading font-semibold text-reimbursement">{initials(claim.personDisplayName)}</span>
                    <div><h2 className="font-heading font-semibold">{claim.personDisplayName}</h2><p className="text-xs text-muted-foreground">{text('claim')}</p></div>
                  </div>
                  <ClaimStatusBadge status={claim.status} label={reimbursementStatusLabel(claim.status, locale)} />
                </div>

                <div className="mt-5 rounded-2xl border bg-muted/25 p-4">
                  <p className="text-xs font-semibold text-muted-foreground">{text('sourceExpense')}</p>
                  <h3 className="mt-1 font-heading text-lg font-semibold">{claim.sourceExpense.description}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {categories.isPending ? text('loadingCategory') : categoryLabelForId(categories.data, claim.sourceExpense.categoryId, locale)} · {formatDate(claim.sourceExpense.eventDate)}
                    {claim.sourceExpense.installmentCount > 1 && ` · ${reimbursementCountText(locale, 'installments', claim.sourceExpense.installmentCount)}`}
                  </p>
                  {claim.sourceExpense.firstOccurrenceDate !== claim.sourceExpense.eventDate && (
                    <p className="mt-1 text-xs text-muted-foreground">{text('firstCashFlowDate', { date: formatDate(claim.sourceExpense.firstOccurrenceDate) })}</p>
                  )}
                </div>

                <div className="mt-3 grid gap-3 rounded-2xl bg-muted/45 p-4 sm:grid-cols-2 xl:grid-cols-4">
                  <div><p className="text-xs text-muted-foreground">{text('sourceExpense')}</p><p className="tabular-nums mt-1 font-heading font-semibold">{formatMoney(claim.sourceExpense.amount)}</p></div>
                  <div><p className="text-xs text-muted-foreground">{text('originallyOwed')}</p><p className="tabular-nums mt-1 font-heading font-semibold">{formatMoney(claim.originalAmount)}</p></div>
                  <div><p className="text-xs text-muted-foreground">{text('reimbursed')}</p><p className="tabular-nums mt-1 font-heading font-semibold text-income">{formatMoney(claim.amountReimbursed)}</p></div>
                  <div><p className="text-xs text-muted-foreground">{text('remaining')}</p><p className="tabular-nums mt-1 font-heading font-semibold text-reimbursement">{formatMoney(claim.remainingAmount)}</p></div>
                </div>

                {claim.note && <p className="mt-4 text-sm text-muted-foreground">{claim.note}</p>}
                {claim.payments.length > 0 && (
                  <details className="group mt-4 rounded-xl border px-3 py-2">
                    <summary className="flex cursor-pointer list-none items-center gap-2 text-xs font-semibold text-muted-foreground"><History className="size-3.5" aria-hidden="true" /> {text('paymentHistory')}</summary>
                    <ul className="mt-3 space-y-2 border-t pt-3">
                      {claim.payments.map((claimPayment) => (
                        <li key={claimPayment.id} className="flex justify-between gap-3 text-xs"><span>{formatDate(claimPayment.receivedDate)}{claimPayment.note ? ` · ${claimPayment.note}` : ''}</span><span className="tabular-nums font-semibold text-income">{formatMoney(claimPayment.amount)}</span></li>
                      ))}
                    </ul>
                  </details>
                )}

                {claim.status !== 'PAID' && activeClaimId !== claim.id && (
                  <Button variant="outline" className="mt-4" onClick={() => { payment.reset(); setActiveClaimId(claim.id) }}>{text('recordRepayment')}</Button>
                )}
                {activeClaimId === claim.id && (
                  <form className="mt-4 grid gap-3 rounded-2xl border border-reimbursement/20 bg-reimbursement-soft p-4 sm:grid-cols-2" onSubmit={(event) => recordPayment(event, claim.id)}>
                    <div className="space-y-2"><Label htmlFor={`payment-amount-${claim.id}`}>{text('amount')}</Label><Input id={`payment-amount-${claim.id}`} required inputMode="decimal" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder={locale === 'pt-BR' ? '0,00' : '0.00'} /></div>
                    <div className="space-y-2"><Label htmlFor={`payment-date-${claim.id}`}>{text('receivedDate')}</Label><Input id={`payment-date-${claim.id}`} required type="date" value={receivedDate} onChange={(event) => setReceivedDate(event.target.value)} /></div>
                    <div className="space-y-2 sm:col-span-2"><Label htmlFor={`payment-note-${claim.id}`}>{text('noteOptional')}</Label><Input id={`payment-note-${claim.id}`} value={note} onChange={(event) => setNote(event.target.value)} /></div>
                    {payment.error && <Alert variant="destructive" className="sm:col-span-2"><AlertDescription>{text('repaymentFailed')}</AlertDescription></Alert>}
                    <div className="flex gap-2 sm:col-span-2"><Button type="submit" disabled={payment.isPending}>{payment.isPending ? text('recording') : text('recordRepayment')}</Button><Button type="button" variant="ghost" onClick={() => setActiveClaimId(null)}>{text('cancel')}</Button></div>
                  </form>
                )}
              </article>
            ))}
          </div>
          {claims.data && claims.data.totalPages > 1 && (
            <nav
              className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t pt-4"
              aria-label={text('claimsPaginationLabel')}
            >
              <Button
                type="button"
                variant="outline"
                disabled={claims.data.page === 0}
                onClick={() => setClaimsPage((current) => Math.max(0, current - 1))}
              >
                {text('previous')}
              </Button>
              <span className="text-xs text-muted-foreground">
                {text('pageOf', {
                  page: claims.data.page + 1,
                  total: claims.data.totalPages,
                })}
              </span>
              <Button
                type="button"
                variant="outline"
                disabled={claims.data.page + 1 >= claims.data.totalPages}
                onClick={() => setClaimsPage((current) => (
                  Math.min(current + 1, claims.data.totalPages - 1)
                ))}
              >
                {text('next')}
              </Button>
            </nav>
          )}
        </CardContent>
      </Card>
    </section>
  )
}
