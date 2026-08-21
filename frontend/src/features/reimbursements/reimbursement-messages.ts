import type { AppLocale } from '@/app/providers/locale-provider'
import type { ReimbursementStatus } from '@/types/api'

const enUS = {
  eyebrow: 'Reimbursements',
  title: 'Owed to Me',
  subtitle: 'Money returned to you stays separate from earned income.',
  totalOutstanding: 'Total outstanding',
  openClaims: 'Open claims',
  people: 'People',
  peopleDescription: 'See who owes you and how much is still outstanding.',
  loadingPeople: 'Loading people',
  peopleUnavailable: 'People unavailable',
  peopleUnavailableDescription: 'People and their summaries could not be loaded.',
  noPeople: 'No people yet',
  noPeopleDescription: 'People will appear here when reimbursement relationships are created.',
  openClaimsCount: '{count} open claims',
  outstanding: 'Outstanding',
  claims: 'Claims',
  claimsDescription: 'Track repayment progress and outstanding balances.',
  loadingClaims: 'Loading reimbursement claims',
  claimsUnavailable: 'Claims unavailable',
  claimsUnavailableDescription: 'Reimbursement claims could not be loaded.',
  noClaims: 'No reimbursement claims',
  noClaimsDescription: 'Nothing is currently owed to you.',
  claim: 'Reimbursement claim',
  sourceExpense: 'Source expense',
  originallyOwed: 'Originally owed',
  reimbursed: 'Reimbursed',
  remaining: 'Remaining',
  installments: '{count} installments',
  firstCashFlowDate: 'First cash-flow date: {date}',
  loadingCategory: 'Loading category…',
  paymentHistory: 'Payment history',
  recordRepayment: 'Record repayment',
  amount: 'Amount',
  receivedDate: 'Received date',
  noteOptional: 'Note (optional)',
  repaymentFailed: 'Repayment could not be recorded.',
  recording: 'Recording…',
  cancel: 'Cancel',
} as const

type ReimbursementMessageKey = keyof typeof enUS

const ptBR: Record<ReimbursementMessageKey, string> = {
  eyebrow: 'Reembolsos',
  title: 'A receber',
  subtitle: 'O dinheiro devolvido a você permanece separado da renda recebida.',
  totalOutstanding: 'Total pendente',
  openClaims: 'Cobranças abertas',
  people: 'Pessoas',
  peopleDescription: 'Veja quem deve a você e quanto ainda está pendente.',
  loadingPeople: 'Carregando pessoas',
  peopleUnavailable: 'Pessoas indisponíveis',
  peopleUnavailableDescription: 'Não foi possível carregar as pessoas e seus resumos.',
  noPeople: 'Nenhuma pessoa ainda',
  noPeopleDescription: 'As pessoas aparecerão aqui quando houver relações de reembolso.',
  openClaimsCount: '{count} cobranças abertas',
  outstanding: 'Pendente',
  claims: 'Cobranças',
  claimsDescription: 'Acompanhe o progresso dos pagamentos e os valores pendentes.',
  loadingClaims: 'Carregando cobranças de reembolso',
  claimsUnavailable: 'Cobranças indisponíveis',
  claimsUnavailableDescription: 'Não foi possível carregar as cobranças de reembolso.',
  noClaims: 'Nenhuma cobrança de reembolso',
  noClaimsDescription: 'No momento, ninguém deve nada a você.',
  claim: 'Cobrança de reembolso',
  sourceExpense: 'Despesa de origem',
  originallyOwed: 'Valor originalmente devido',
  reimbursed: 'Reembolsado',
  remaining: 'Restante',
  installments: '{count} parcelas',
  firstCashFlowDate: 'Primeiro fluxo de caixa: {date}',
  loadingCategory: 'Carregando categoria…',
  paymentHistory: 'Histórico de pagamentos',
  recordRepayment: 'Registrar pagamento',
  amount: 'Valor',
  receivedDate: 'Data de recebimento',
  noteOptional: 'Observação (opcional)',
  repaymentFailed: 'Não foi possível registrar o pagamento.',
  recording: 'Registrando…',
  cancel: 'Cancelar',
}

const messages: Record<AppLocale, Record<ReimbursementMessageKey, string>> = {
  'en-US': enUS,
  'pt-BR': ptBR,
}

function interpolate(message: string, params?: Record<string, string | number>): string {
  if (!params) return message
  return message.replace(/\{(\w+)\}/g, (match, key: string) => (
    Object.prototype.hasOwnProperty.call(params, key) ? String(params[key]) : match
  ))
}

export function reimbursementText(
  locale: AppLocale,
  key: ReimbursementMessageKey,
  params?: Record<string, string | number>,
): string {
  return interpolate(messages[locale][key], params)
}

export function reimbursementStatusLabel(status: ReimbursementStatus, locale: AppLocale): string {
  const labels: Record<ReimbursementStatus, Record<AppLocale, string>> = {
    PENDING: { 'en-US': 'Pending', 'pt-BR': 'Pendente' },
    PARTIALLY_PAID: { 'en-US': 'Partially paid', 'pt-BR': 'Parcialmente pago' },
    PAID: { 'en-US': 'Paid', 'pt-BR': 'Pago' },
  }
  return labels[status][locale]
}
