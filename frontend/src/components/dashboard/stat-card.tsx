import type { LucideIcon } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { cn } from '@/lib/utils'

interface StatCardProps {
  title: string
  value: string
  note: string
  icon: LucideIcon
  tone: 'income' | 'expense' | 'reimbursement' | 'primary'
}

const tones = {
  income: 'bg-income-soft text-income',
  expense: 'bg-expense-soft text-expense',
  reimbursement: 'bg-reimbursement-soft text-reimbursement',
  primary: 'bg-accent text-primary',
}

export function StatCard({ title, value, note, icon: Icon, tone }: StatCardProps) {
  return (
    <Card className="min-h-40">
      <CardContent className="flex h-full flex-col justify-between gap-5">
        <div className="flex items-start justify-between gap-3">
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <span className={cn('grid size-9 place-items-center rounded-xl', tones[tone])}>
            <Icon className="size-4" aria-hidden="true" />
          </span>
        </div>
        <div>
          <p className="tabular-nums font-heading text-2xl font-semibold tracking-[-0.04em] sm:text-3xl">{value}</p>
          <p className="mt-1 text-xs leading-relaxed text-muted-foreground">{note}</p>
        </div>
      </CardContent>
    </Card>
  )
}
