import type { TransactionKind } from '@/types/api'

const styles: Record<TransactionKind, { className: string; prefix: string }> = {
  INCOME: { className: 'text-income', prefix: '+' },
  EXPENSE: { className: 'text-expense', prefix: '−' },
  REIMBURSEMENT_RECEIPT: { className: 'text-reimbursement', prefix: '+' },
}

export function financialAmountStyle(kind: TransactionKind) {
  return styles[kind]
}
