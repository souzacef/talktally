import { lazy, Suspense } from 'react'
import { ArrowDownLeft, ArrowUpRight, HandCoins, RotateCcw, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { VoiceOrb } from '@/components/assistant/voice-orb'
import { StatCard } from '@/components/dashboard/stat-card'
import { StatePanel } from '@/components/feedback/state-panel'
import { KindIcon } from '@/components/finance/financial-visuals'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { peopleApi } from '@/features/reimbursements/api/people-api'
import { transactionApi } from '@/features/transactions/api/transaction-api'
import { useVoiceAssistant } from '@/features/assistant/hooks/use-voice-assistant'
import {
  useCategoryBreakdown,
  useFinancialSummary,
  useMonthlyCashFlow,
} from '@/features/dashboard/hooks/use-dashboard'
import { currentMonthRange, trailingSixMonthRange } from '@/lib/dates/reporting-periods'
import { formatBrl } from '@/lib/money/format-brl'
import { financialAmountStyle } from '@/lib/money/financial-display'
import { queryKeys } from '@/lib/query/query-client'

const CashFlowChart = lazy(() => import('@/components/dashboard/cash-flow-chart').then((module) => ({
  default: module.CashFlowChart,
})))

const recentParams = { page: 0, size: 5 } as const

export function DashboardPage() {
  const period = currentMonthRange()
  const history = trailingSixMonthRange()
  const summary = useFinancialSummary(period.from, period.to)
  const breakdown = useCategoryBreakdown(period.from, period.to, 'EXPENSE')
  const cashFlow = useMonthlyCashFlow(history.from, history.to)
  const transactions = useQuery({
    queryKey: queryKeys.transactions.list(recentParams),
    queryFn: ({ signal }) => transactionApi.list(recentParams, signal),
  })
  const people = useQuery({
    queryKey: queryKeys.people.all,
    queryFn: ({ signal }) => peopleApi.list(signal),
  })
  const voice = useVoiceAssistant()

  function toggleVoice() {
    if (voice.isRecording) voice.stopRecording()
    else void voice.startRecording()
  }

  return (
    <section className="space-y-7">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-xs font-bold tracking-[0.14em] text-primary uppercase">This month</p>
          <h1 className="font-heading text-3xl font-semibold tracking-[-0.045em] sm:text-4xl">Hello</h1>
          <p className="mt-2 text-muted-foreground">Here is the story your money is telling.</p>
        </div>
        <p className="rounded-full border bg-card px-3 py-1.5 text-xs font-medium text-muted-foreground shadow-xs">
          {period.from} — {period.to}
        </p>
      </header>

      <div className="grid min-w-0 gap-4 lg:grid-cols-3">
        <Card className="relative min-h-80 overflow-hidden border-primary/15 bg-[linear-gradient(145deg,var(--card),var(--accent))] lg:row-span-2">
          <div className="pointer-events-none absolute -right-16 -top-16 size-52 rounded-full bg-primary/10 blur-2xl" aria-hidden="true" />
          <CardContent className="relative flex h-full flex-col items-center justify-center py-7 text-center">
            <span className="mb-4 rounded-full border border-primary/15 bg-card/75 px-3 py-1 text-xs font-semibold text-primary">Voice-first capture</span>
            <VoiceOrb state={voice.state} onClick={toggleVoice} />
            <h2 className="mt-5 font-heading text-2xl font-semibold tracking-[-0.035em]">What happened today?</h2>
            <p className="mt-2 max-w-xs text-sm leading-relaxed text-muted-foreground">
              Speak naturally. TalkTally sends a real WAV command to your secured assistant.
            </p>
            {voice.error && <p className="mt-3 text-sm text-expense" role="alert">{voice.error}</p>}
            {voice.result && (
              <div className="mt-4 w-full rounded-2xl border bg-card/80 p-3 text-left text-sm" aria-live="polite">
                <p className="font-semibold">{voice.result.message}</p>
                <p className="mt-1 text-xs text-muted-foreground">Heard: {voice.result.transcript}</p>
                {voice.result.speechStatus === 'UNAVAILABLE' && (
                  <p className="mt-2 text-xs text-protected">Voice reply unavailable — result still succeeded.</p>
                )}
              </div>
            )}
            <Link to="/assistant" className="mt-5 text-sm font-semibold text-primary underline-offset-4 hover:underline">Type instead</Link>
          </CardContent>
        </Card>

        {summary.isPending && Array.from({ length: 4 }, (_, index) => (
          <div key={index} className="min-h-40 animate-pulse rounded-2xl border bg-card shadow-[var(--shadow-soft)]" aria-hidden="true" />
        ))}
        {summary.error && (
          <StatePanel className="lg:col-span-2" tone="error" title="Summary unavailable" description="Your financial totals could not be loaded right now." />
        )}
        {summary.data && (
          <>
            <StatCard title="Earned income" value={formatBrl(summary.data.period.earnedIncome)} note="Income recorded for this period" icon={ArrowDownLeft} tone="income" />
            <StatCard title="Expenses" value={formatBrl(summary.data.period.expenses)} note="Spending recorded for this period" icon={ArrowUpRight} tone="expense" />
            <StatCard title="Reimbursements received" value={formatBrl(summary.data.period.reimbursementsReceived)} note="Money returned to you — not income" icon={RotateCcw} tone="reimbursement" />
            <StatCard title="Net cash flow" value={formatBrl(summary.data.period.netCashFlow)} note="Calculated by TalkTally on the server" icon={Sparkles} tone="primary" />
          </>
        )}
      </div>

      <div className="grid min-w-0 gap-4 lg:grid-cols-3">
        <Card className="min-w-0 lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-xl">Monthly cash flow</CardTitle>
            <CardDescription>Backend-reported income and expenses across the last six calendar months.</CardDescription>
          </CardHeader>
          <CardContent>
            {cashFlow.isPending && <div className="h-64 animate-pulse rounded-2xl bg-muted" aria-label="Loading monthly cash flow" />}
            {cashFlow.error && <StatePanel tone="error" title="Cash flow unavailable" description="The monthly series could not be loaded." />}
            {cashFlow.data?.buckets.length === 0 && <StatePanel title="No cash-flow activity" description="Your monthly trend will appear after transactions are recorded." />}
            {cashFlow.data && cashFlow.data.buckets.length > 0 && (
              <Suspense fallback={<div className="h-64 animate-pulse rounded-2xl bg-muted" aria-label="Loading cash-flow chart" />}>
                <CashFlowChart buckets={cashFlow.data.buckets} />
              </Suspense>
            )}
          </CardContent>
        </Card>

        <Card className="border-primary bg-primary text-primary-foreground shadow-[var(--shadow-lift)]">
          <CardContent className="flex h-full min-h-72 flex-col justify-between">
            <div className="flex items-center justify-between">
              <span className="rounded-full bg-white/12 px-3 py-1 text-xs font-semibold">Owed to me</span>
              <HandCoins className="size-6 opacity-80" aria-hidden="true" />
            </div>
            <div className="py-7">
              <p className="text-sm opacity-75">Total outstanding</p>
              <p className="tabular-nums mt-2 font-heading text-4xl font-semibold tracking-[-0.05em]">
                {summary.data ? formatBrl(summary.data.owedToMe.outstanding) : '—'}
              </p>
            </div>
            <div className="grid grid-cols-2 gap-3 border-t border-white/15 pt-4">
              <div><p className="font-heading text-xl font-semibold">{summary.data?.owedToMe.openClaims ?? '—'}</p><p className="text-xs opacity-70">Open claims</p></div>
              <div><p className="font-heading text-xl font-semibold">{people.data?.length ?? '—'}</p><p className="text-xs opacity-70">People</p></div>
            </div>
            <Link to="/owed" className="mt-5 text-sm font-semibold underline decoration-white/40 underline-offset-4">View reimbursements</Link>
          </CardContent>
        </Card>
      </div>

      <div className="grid min-w-0 gap-4 lg:grid-cols-3">
        <Card>
          <CardHeader><CardTitle className="text-xl">Spending by category</CardTitle><CardDescription>This month</CardDescription></CardHeader>
          <CardContent>
            {breakdown.isPending && <div className="h-36 animate-pulse rounded-2xl bg-muted" />}
            {breakdown.error && <StatePanel tone="error" title="Categories unavailable" description="Category reporting could not be loaded." />}
            {breakdown.data?.categories.length === 0 && <StatePanel title="No expenses yet" description="Category totals will appear when expenses are recorded." />}
            <ul className="space-y-4">
              {breakdown.data?.categories.map((category) => (
                <li key={category.categoryId} className="flex items-center justify-between gap-4">
                  <div className="min-w-0"><p className="truncate font-semibold">{category.displayName}</p><p className="text-xs text-muted-foreground">{category.occurrenceCount} occurrences</p></div>
                  <span className="tabular-nums font-heading font-semibold text-expense">{formatBrl(category.total)}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader><CardTitle className="text-xl">Recent activity</CardTitle><CardDescription>Latest server-owned financial records</CardDescription></CardHeader>
          <CardContent>
            {transactions.isPending && <div className="h-36 animate-pulse rounded-2xl bg-muted" />}
            {transactions.error && <StatePanel tone="error" title="Activity unavailable" description="Recent transactions could not be loaded." />}
            {transactions.data?.items.length === 0 && <StatePanel title="No transactions yet" description="Use voice or the assistant to record your first transaction." />}
            <ul className="divide-y">
              {transactions.data?.items.map((transaction) => {
                const amount = financialAmountStyle(transaction.kind)
                return (
                  <li key={transaction.id} className="flex items-center gap-3 py-3 first:pt-0 last:pb-0">
                    <KindIcon kind={transaction.kind} />
                    <div className="min-w-0 flex-1"><p className="truncate font-semibold">{transaction.description}</p><p className="text-xs text-muted-foreground">{transaction.eventDate}</p></div>
                    <span className={`tabular-nums font-heading font-semibold ${amount.className}`}>{amount.prefix}{formatBrl(transaction.amount)}</span>
                  </li>
                )
              })}
            </ul>
          </CardContent>
        </Card>
      </div>
    </section>
  )
}
