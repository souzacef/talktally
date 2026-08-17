import {
  ArrowDownLeft,
  ArrowUpRight,
  LockKeyhole,
  RotateCcw,
  type LucideIcon,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ReimbursementStatus, TransactionKind } from '@/types/api'

const kindStyles: Record<TransactionKind, {
  label: string
  icon: LucideIcon
  foreground: string
  background: string
  prefix: string
}> = {
  INCOME: {
    label: 'Income',
    icon: ArrowDownLeft,
    foreground: 'text-income',
    background: 'bg-income-soft',
    prefix: '+',
  },
  EXPENSE: {
    label: 'Expense',
    icon: ArrowUpRight,
    foreground: 'text-expense',
    background: 'bg-expense-soft',
    prefix: '−',
  },
  REIMBURSEMENT_RECEIPT: {
    label: 'Reimbursement',
    icon: RotateCcw,
    foreground: 'text-reimbursement',
    background: 'bg-reimbursement-soft',
    prefix: '+',
  },
}

const claimStyles: Record<ReimbursementStatus, string> = {
  PENDING: 'bg-warning-soft text-warning',
  PARTIALLY_PAID: 'bg-reimbursement-soft text-reimbursement',
  PAID: 'bg-income-soft text-income',
}

export function KindIcon({ kind, className }: { kind: TransactionKind; className?: string }) {
  const style = kindStyles[kind]
  const Icon = style.icon
  return (
    <span className={cn('grid size-10 shrink-0 place-items-center rounded-xl', style.background, style.foreground, className)}>
      <Icon className="size-4.5" aria-hidden="true" />
    </span>
  )
}

export function KindBadge({ kind }: { kind: TransactionKind }) {
  const style = kindStyles[kind]
  return (
    <span className={cn('inline-flex items-center rounded-full px-2.5 py-1 text-[0.68rem] font-semibold tracking-wide uppercase', style.background, style.foreground)}>
      {style.label}
    </span>
  )
}

export function ProtectedBadge() {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-protected-soft px-2.5 py-1 text-[0.68rem] font-semibold text-protected">
      <LockKeyhole className="size-3" aria-hidden="true" /> Protected
    </span>
  )
}

export function ClaimStatusBadge({ status }: { status: ReimbursementStatus }) {
  return (
    <span className={cn('inline-flex rounded-full px-2.5 py-1 text-[0.68rem] font-semibold tracking-wide uppercase', claimStyles[status])}>
      {status.replace('_', ' ')}
    </span>
  )
}
