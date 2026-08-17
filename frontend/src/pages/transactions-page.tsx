import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { transactionApi } from '@/features/transactions/api/transaction-api'
import { formatBrl } from '@/lib/money/format-brl'
import { queryKeys } from '@/lib/query/query-client'

const defaultParams = { page: 0, size: 20 } as const

export function TransactionsPage() {
  const query = useQuery({
    queryKey: queryKeys.transactions.list(defaultParams),
    queryFn: ({ signal }) => transactionApi.list(defaultParams, signal),
  })

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Transactions</h1>
        <p className="text-muted-foreground">Your latest server-owned financial records.</p>
      </div>
      <Card>
        <CardHeader><CardTitle>Recent transactions</CardTitle></CardHeader>
        <CardContent>
          {query.isPending && <p>Loading transactions…</p>}
          {query.error && <p>Transactions could not be loaded.</p>}
          {query.data?.items.length === 0 && <p>No transactions yet.</p>}
          <ul className="divide-y">
            {query.data?.items.map((transaction) => (
              <li key={transaction.id} className="flex items-start justify-between gap-4 py-3">
                <div>
                  <p className="font-medium">{transaction.description}</p>
                  <p className="text-sm text-muted-foreground">{transaction.eventDate} · {transaction.kind}</p>
                </div>
                <span className="font-medium">{formatBrl(transaction.amount)}</span>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </section>
  )
}
