import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { formatBrl } from '@/lib/money/format-brl'
import type { MonthlyCashFlowResponse } from '@/types/api'

export function CashFlowChart({ buckets }: { buckets: MonthlyCashFlowResponse['buckets'] }) {
  // Recharts requires numeric display coordinates. These are direct conversions
  // of backend-computed values; no totals or financial arithmetic happen here.
  const data = buckets.map((bucket) => ({
    label: new Intl.DateTimeFormat('en', { month: 'short' }).format(new Date(bucket.year, bucket.month - 1, 1)),
    earnedIncome: Number(bucket.earnedIncome),
    expenses: Number(bucket.expenses),
  }))

  return (
    <div className="h-64 min-w-0" aria-label="Monthly cash flow chart">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 10, right: 4, left: -18, bottom: 0 }} accessibilityLayer>
          <defs>
            <linearGradient id="income-fill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--income)" stopOpacity={0.28} />
              <stop offset="100%" stopColor="var(--income)" stopOpacity={0} />
            </linearGradient>
            <linearGradient id="expense-fill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--expense)" stopOpacity={0.22} />
              <stop offset="100%" stopColor="var(--expense)" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid vertical={false} stroke="var(--border)" strokeDasharray="3 5" />
          <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: 'var(--muted-foreground)', fontSize: 12 }} />
          <YAxis axisLine={false} tickLine={false} tick={{ fill: 'var(--muted-foreground)', fontSize: 11 }} width={58} />
          <Tooltip
            cursor={{ stroke: 'var(--border)' }}
            contentStyle={{
              background: 'var(--popover)',
              border: '1px solid var(--border)',
              borderRadius: '0.9rem',
              color: 'var(--popover-foreground)',
              boxShadow: 'var(--shadow-soft)',
            }}
            formatter={(value, name) => [
              formatBrl(String(value)),
              name === 'earnedIncome' ? 'Income' : 'Expenses',
            ]}
          />
          <Area type="monotone" dataKey="earnedIncome" stroke="var(--income)" fill="url(#income-fill)" strokeWidth={2} />
          <Area type="monotone" dataKey="expenses" stroke="var(--expense)" fill="url(#expense-fill)" strokeWidth={2} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
