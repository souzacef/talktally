import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  useCategoryBreakdown,
  useFinancialSummary,
  useMonthlyCashFlow,
} from '@/features/dashboard/hooks/use-dashboard'
import { formatBrl } from '@/lib/money/format-brl'

function currentMonthRange(): { from: string; to: string } {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const from = `${year}-${String(month + 1).padStart(2, '0')}-01`
  const finalDay = new Date(year, month + 1, 0).getDate()
  return { from, to: `${year}-${String(month + 1).padStart(2, '0')}-${finalDay}` }
}

export function DashboardPage() {
  const { from, to } = currentMonthRange()
  const summary = useFinancialSummary(from, to)
  const breakdown = useCategoryBreakdown(from, to, 'EXPENSE')
  const cashFlow = useMonthlyCashFlow(from, to)

  if (summary.isPending) return <p aria-live="polite">Loading dashboard…</p>
  if (summary.error) {
    return <Alert variant="destructive"><AlertDescription>Dashboard data could not be loaded.</AlertDescription></Alert>
  }

  const data = summary.data
  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">Server-calculated totals for {from} through {to}.</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <SummaryCard title="Earned income" value={formatBrl(data.period.earnedIncome)} />
        <SummaryCard title="Expenses" value={formatBrl(data.period.expenses)} />
        <SummaryCard title="Reimbursements received" value={formatBrl(data.period.reimbursementsReceived)} />
        <SummaryCard title="Net cash flow" value={formatBrl(data.period.netCashFlow)} />
      </div>
      <Card>
        <CardHeader><CardTitle>Owed to me</CardTitle></CardHeader>
        <CardContent>
          <p className="text-2xl font-semibold">{formatBrl(data.owedToMe.outstanding)}</p>
          <p className="text-sm text-muted-foreground">{data.owedToMe.openClaims} open claims</p>
        </CardContent>
      </Card>
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Expense categories</CardTitle></CardHeader>
          <CardContent>
            {breakdown.isPending && <p>Loading categories…</p>}
            {breakdown.error && <p>Category data is unavailable.</p>}
            {breakdown.data?.categories.length === 0 && <p>No expenses in this period.</p>}
            <ul className="space-y-2">
              {breakdown.data?.categories.map((category) => (
                <li key={category.categoryId} className="flex justify-between gap-4">
                  <span>{category.displayName}</span>
                  <span>{formatBrl(category.total)}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Monthly cash flow</CardTitle></CardHeader>
          <CardContent>
            {cashFlow.isPending && <p>Loading cash flow…</p>}
            {cashFlow.error && <p>Cash-flow data is unavailable.</p>}
            {cashFlow.data?.buckets.length === 0 && <p>No activity in this period.</p>}
            <ul className="space-y-2">
              {cashFlow.data?.buckets.map((bucket) => (
                <li key={`${bucket.year}-${bucket.month}`} className="flex justify-between gap-4">
                  <span>{String(bucket.month).padStart(2, '0')}/{bucket.year}</span>
                  <span>{formatBrl(bucket.netCashFlow)}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      </div>
    </section>
  )
}

function SummaryCard({ title, value }: { title: string; value: string }) {
  return (
    <Card>
      <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">{title}</CardTitle></CardHeader>
      <CardContent><p className="text-2xl font-semibold">{value}</p></CardContent>
    </Card>
  )
}
