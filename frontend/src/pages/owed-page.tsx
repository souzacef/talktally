import { useState, type FormEvent } from 'react'
import { HandCoins, History, Users } from 'lucide-react'
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { ClaimStatusBadge } from '@/components/finance/financial-visuals'
import { StatePanel } from '@/components/feedback/state-panel'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { peopleApi } from '@/features/reimbursements/api/people-api'
import { reimbursementApi } from '@/features/reimbursements/api/reimbursement-api'
import { useFinancialSummary } from '@/features/dashboard/hooks/use-dashboard'
import { ApiError } from '@/lib/api/api-client'
import { currentMonthRange } from '@/lib/dates/reporting-periods'
import { formatBrl } from '@/lib/money/format-brl'
import { queryKeys } from '@/lib/query/query-client'
import type { ReimbursementPaymentRequest } from '@/types/api'

const claimsParams = { page: 0, size: 20 } as const

function initials(displayName: string): string {
  return displayName.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || '?'
}

export function OwedPage() {
  const queryClient = useQueryClient()
  const period = currentMonthRange()
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
    payment.mutate({ claimId, request: { amount, receivedDate, note: note.trim() || null } })
  }

  return (
    <section className="space-y-6">
      <header>
        <p className="mb-1 text-xs font-bold tracking-[0.14em] text-reimbursement uppercase">Reimbursements</p>
        <h1 className="font-heading text-3xl font-semibold tracking-[-0.045em] sm:text-4xl">Owed to Me</h1>
        <p className="mt-2 text-muted-foreground">Money returned to you stays separate from earned income.</p>
      </header>

      <Card className="border-primary bg-primary text-primary-foreground shadow-[var(--shadow-lift)]">
        <CardContent className="grid gap-6 py-2 sm:grid-cols-[1.5fr_1fr_1fr] sm:items-end">
          <div>
            <div className="mb-6 flex items-center gap-2 text-sm font-semibold"><HandCoins className="size-5" aria-hidden="true" /> Total outstanding</div>
            <p className="tabular-nums font-heading text-4xl font-semibold tracking-[-0.05em] sm:text-5xl">
              {summary.data ? formatBrl(summary.data.owedToMe.outstanding) : '—'}
            </p>
          </div>
          <div className="border-t border-white/15 pt-4 sm:border-l sm:border-t-0 sm:pl-6 sm:pt-0">
            <p className="font-heading text-3xl font-semibold">{summary.data?.owedToMe.openClaims ?? '—'}</p>
            <p className="mt-1 text-sm opacity-70">Open claims</p>
          </div>
          <div className="border-t border-white/15 pt-4 sm:border-l sm:border-t-0 sm:pl-6 sm:pt-0">
            <p className="font-heading text-3xl font-semibold">{people.data?.length ?? '—'}</p>
            <p className="mt-1 text-sm opacity-70">People</p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2 text-xl"><Users className="size-5 text-reimbursement" aria-hidden="true" /> People</CardTitle><CardDescription>Backend-derived reimbursement summaries by person</CardDescription></CardHeader>
        <CardContent>
          {people.isPending && <div className="h-32 animate-pulse rounded-2xl bg-muted" aria-label="Loading people" />}
          {people.error && <StatePanel tone="error" title="People unavailable" description="People and their summaries could not be loaded." />}
          {people.data?.length === 0 && <StatePanel title="No people yet" description="People will appear here when reimbursement relationships are created." />}
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {people.data?.map((person, index) => {
              const personSummary = personSummaries[index]
              return (
                <article key={person.id} className="rounded-2xl border bg-muted/25 p-4">
                  <div className="flex items-center gap-3">
                    <span className="grid size-11 place-items-center rounded-full bg-reimbursement-soft font-heading font-semibold text-reimbursement">{initials(person.displayName)}</span>
                    <div className="min-w-0"><h2 className="truncate font-heading font-semibold">{person.displayName}</h2><p className="text-xs text-muted-foreground">{personSummary.data?.openClaimCount ?? '—'} open claims</p></div>
                  </div>
                  <p className="tabular-nums mt-5 font-heading text-xl font-semibold text-reimbursement">
                    {personSummary.data ? formatBrl(personSummary.data.totalOutstanding) : '—'}
                  </p>
                  <p className="text-xs text-muted-foreground">Outstanding</p>
                </article>
              )
            })}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="text-xl">Claims</CardTitle><CardDescription>Statuses and amounts are supplied by the backend.</CardDescription></CardHeader>
        <CardContent>
          {claims.isPending && <div className="h-48 animate-pulse rounded-2xl bg-muted" aria-label="Loading reimbursement claims" />}
          {claims.error && <StatePanel tone="error" title="Claims unavailable" description="Reimbursement claims could not be loaded." />}
          {claims.data?.items.length === 0 && <StatePanel title="No reimbursement claims" description="Nothing is currently owed to you." />}
          <div className="space-y-4">
            {claims.data?.items.map((claim) => (
              <article key={claim.id} className="rounded-2xl border bg-card p-4 shadow-xs sm:p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="grid size-10 shrink-0 place-items-center rounded-full bg-reimbursement-soft font-heading font-semibold text-reimbursement">{initials(claim.personDisplayName)}</span>
                    <div><h2 className="font-heading font-semibold">{claim.personDisplayName}</h2><p className="text-xs text-muted-foreground">Reimbursement claim</p></div>
                  </div>
                  <ClaimStatusBadge status={claim.status} />
                </div>

                <div className="mt-5 grid gap-3 rounded-2xl bg-muted/45 p-4 sm:grid-cols-3">
                  <div><p className="text-xs text-muted-foreground">Original</p><p className="tabular-nums mt-1 font-heading font-semibold">{formatBrl(claim.originalAmount)}</p></div>
                  <div><p className="text-xs text-muted-foreground">Reimbursed</p><p className="tabular-nums mt-1 font-heading font-semibold text-income">{formatBrl(claim.amountReimbursed)}</p></div>
                  <div><p className="text-xs text-muted-foreground">Remaining</p><p className="tabular-nums mt-1 font-heading font-semibold text-reimbursement">{formatBrl(claim.remainingAmount)}</p></div>
                </div>

                {claim.note && <p className="mt-4 text-sm text-muted-foreground">{claim.note}</p>}
                {claim.payments.length > 0 && (
                  <details className="group mt-4 rounded-xl border px-3 py-2">
                    <summary className="flex cursor-pointer list-none items-center gap-2 text-xs font-semibold text-muted-foreground"><History className="size-3.5" aria-hidden="true" /> Payment history</summary>
                    <ul className="mt-3 space-y-2 border-t pt-3">
                      {claim.payments.map((claimPayment) => (
                        <li key={claimPayment.id} className="flex justify-between gap-3 text-xs"><span>{claimPayment.receivedDate}{claimPayment.note ? ` · ${claimPayment.note}` : ''}</span><span className="tabular-nums font-semibold text-income">{formatBrl(claimPayment.amount)}</span></li>
                      ))}
                    </ul>
                  </details>
                )}

                {claim.status !== 'PAID' && activeClaimId !== claim.id && (
                  <Button variant="outline" className="mt-4" onClick={() => { payment.reset(); setActiveClaimId(claim.id) }}>Record repayment</Button>
                )}
                {activeClaimId === claim.id && (
                  <form className="mt-4 grid gap-3 rounded-2xl border border-reimbursement/20 bg-reimbursement-soft p-4 sm:grid-cols-2" onSubmit={(event) => recordPayment(event, claim.id)}>
                    <div className="space-y-2"><Label htmlFor={`payment-amount-${claim.id}`}>Amount</Label><Input id={`payment-amount-${claim.id}`} required inputMode="decimal" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="0.00" /></div>
                    <div className="space-y-2"><Label htmlFor={`payment-date-${claim.id}`}>Received date</Label><Input id={`payment-date-${claim.id}`} required type="date" value={receivedDate} onChange={(event) => setReceivedDate(event.target.value)} /></div>
                    <div className="space-y-2 sm:col-span-2"><Label htmlFor={`payment-note-${claim.id}`}>Note (optional)</Label><Input id={`payment-note-${claim.id}`} value={note} onChange={(event) => setNote(event.target.value)} /></div>
                    {payment.error && <Alert variant="destructive" className="sm:col-span-2"><AlertDescription>{payment.error instanceof ApiError ? payment.error.message : 'Repayment could not be recorded'}</AlertDescription></Alert>}
                    <div className="flex gap-2 sm:col-span-2"><Button type="submit" disabled={payment.isPending}>{payment.isPending ? 'Recording…' : 'Record repayment'}</Button><Button type="button" variant="ghost" onClick={() => setActiveClaimId(null)}>Cancel</Button></div>
                  </form>
                )}
              </article>
            ))}
          </div>
        </CardContent>
      </Card>
    </section>
  )
}
