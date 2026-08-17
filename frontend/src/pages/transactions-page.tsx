import { useState, type FormEvent } from 'react'
import { ChevronDown, Filter, Search } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { StatePanel } from '@/components/feedback/state-panel'
import {
  KindBadge,
  KindIcon,
  ProtectedBadge,
} from '@/components/finance/financial-visuals'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { transactionApi } from '@/features/transactions/api/transaction-api'
import { formatBrl } from '@/lib/money/format-brl'
import { queryKeys } from '@/lib/query/query-client'
import type { TransactionKind, TransactionListParams } from '@/types/api'
import { financialAmountStyle } from '@/lib/money/financial-display'

const initialParams: TransactionListParams = { page: 0, size: 20 }

export function TransactionsPage() {
  const [search, setSearch] = useState('')
  const [kind, setKind] = useState<TransactionKind | ''>('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [params, setParams] = useState<TransactionListParams>(initialParams)
  const query = useQuery({
    queryKey: queryKeys.transactions.list(params),
    queryFn: ({ signal }) => transactionApi.list(params, signal),
  })

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setParams({
      page: 0,
      size: 20,
      search: search.trim() || undefined,
      kind: kind || undefined,
      from: from || undefined,
      to: to || undefined,
    })
  }

  function clearFilters() {
    setSearch('')
    setKind('')
    setFrom('')
    setTo('')
    setParams(initialParams)
  }

  return (
    <section className="space-y-6">
      <header>
        <p className="mb-1 text-xs font-bold tracking-[0.14em] text-primary uppercase">Ledger</p>
        <h1 className="font-heading text-3xl font-semibold tracking-[-0.045em] sm:text-4xl">Transactions</h1>
        <p className="mt-2 text-muted-foreground">Search and understand every server-owned financial record.</p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg"><Filter className="size-4 text-primary" aria-hidden="true" /> Find transactions</CardTitle>
          <CardDescription>Reimbursement receipts can be viewed here, but remain server-managed.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4 md:grid-cols-2 xl:grid-cols-[minmax(14rem,1fr)_11rem_10rem_10rem_auto]" onSubmit={applyFilters}>
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
            <div className="space-y-2"><Label htmlFor="transaction-from">From</Label><Input id="transaction-from" type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></div>
            <div className="space-y-2"><Label htmlFor="transaction-to">To</Label><Input id="transaction-to" type="date" value={to} onChange={(event) => setTo(event.target.value)} /></div>
            <div className="flex items-end gap-2 md:col-span-2 xl:col-span-1">
              <Button type="submit" className="flex-1">Apply</Button>
              <Button type="button" variant="ghost" onClick={clearFilters}>Reset</Button>
            </div>
          </form>
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
                        <span>{transaction.installmentCount} installment{transaction.installmentCount === 1 ? '' : 's'}</span>
                        <span title={transaction.categoryId}>Category {transaction.categoryId.slice(0, 8)}</span>
                        <span>{transaction.source.replace('_', ' ').toLowerCase()}</span>
                      </div>
                    </div>
                    <span className={`tabular-nums shrink-0 font-heading text-base font-semibold sm:text-lg ${amount.className}`}>
                      {amount.prefix}{formatBrl(transaction.amount)}
                    </span>
                  </div>
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
