import type { AppLocale } from '@/app/providers/locale-provider'
import type { ReimbursementStatus } from '@/types/api'

const enUS = {
  eyebrow: 'Reimbursements',
  title: 'Owed to Me',
  subtitle: 'Money returned to you stays separate from earned income.',
  totalOutstanding: 'Total outstanding',
  openClaims: 'Open claims',
  people: 'People',
  peopleDescription: 'Backend-derived reimbursement summaries by person',
  loadingPeople: 'Loading people',
  peopleUnavailable: 'People unavailable',
  peopleUnavailableDescription: 'People and their summaries could not be loaded.',
  noPeople: 'No people yet',
  noPeopleDescription: 'People will appear here when reimbursement relationships are created.',
  openClaimsCount: '{count} open claims',
  outstanding: 'Outstanding',
  claims: 'Claims',
  claimsDescription: 'Statuses and amounts are supplied by the backend.',
  loadingClaims: 'Loading reimbursement claims',
  claimsUnavailable: 'Claims unavailable',
  claimsUnavailableDescription: 'Reimbursement claims could not be loaded.',
  noClaims: 'No reimbursement claims',
  noClaimsDescription: 'Nothing is currently owed to you.',
  claim: 'Reimbursement claim',
  original: 'Original',
  reimbursed: 'Reimbursed',
  remaining: 'Remaining',
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
  peopleDescription: 'Resumos de reembolso por pessoa calculados pelo backend',
  loadingPeople: 'Carregando pessoas',
  peopleUnavailable: 'Pessoas indisponíveis',
  peopleUnavailableDescription: 'Não foi possível carregar as pessoas e seus resumos.',
  noPeople: 'Nenhuma pessoa ainda',
  noPeopleDescription: 'As pessoas aparecerão aqui quando houver relações de reembolso.',
  openClaimsCount: '{count} cobranças abertas',
  outstanding: 'Pendente',
  claims: 'Cobranças',
  claimsDescription: 'Os status e valores são fornecidos pelo backend.',
  loadingClaims: 'Carregando cobranças de reembolso',
  claimsUnavailable: 'Cobranças indisponíveis',
  claimsUnavailableDescription: 'Não foi possível carregar as cobranças de reembolso.',
  noClaims: 'Nenhuma cobrança de reembolso',
  noClaimsDescription: 'No momento, ninguém deve nada a você.',
  claim: 'Cobrança de reembolso',
  original: 'Original',
  reimbursed: 'Reembolsado',
  remaining: 'Restante',
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
