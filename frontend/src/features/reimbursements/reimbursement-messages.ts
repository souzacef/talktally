import type { AppLocale } from '@/app/providers/locale-provider'
import { formatPluralMessage } from '@/lib/plural'
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
  outstanding: 'Outstanding',
  claims: 'Claims',
  claimsDescription: 'Track repayment progress and outstanding balances.',
  loadingClaims: 'Loading reimbursement claims',
  claimsUnavailable: 'Claims unavailable',
  claimsUnavailableDescription: 'Reimbursement claims could not be loaded.',
  noClaims: 'No reimbursement claims',
  noClaimsDescription: 'Nothing is currently owed to you.',
  claimsPaginationLabel: 'Reimbursement claim pages',
  previous: 'Previous',
  next: 'Next',
  pageOf: 'Page {page} of {total}',
  claim: 'Reimbursement claim',
  sourceExpense: 'Source expense',
  originallyOwed: 'Originally owed',
  reimbursed: 'Reimbursed',
  remaining: 'Remaining',
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
  outstanding: 'Pendente',
  claims: 'Cobranças',
  claimsDescription: 'Acompanhe o progresso dos pagamentos e os valores pendentes.',
  loadingClaims: 'Carregando cobranças de reembolso',
  claimsUnavailable: 'Cobranças indisponíveis',
  claimsUnavailableDescription: 'Não foi possível carregar as cobranças de reembolso.',
  noClaims: 'Nenhuma cobrança de reembolso',
  noClaimsDescription: 'No momento, ninguém deve nada a você.',
  claimsPaginationLabel: 'Páginas das cobranças de reembolso',
  previous: 'Anterior',
  next: 'Próxima',
  pageOf: 'Página {page} de {total}',
  claim: 'Cobrança de reembolso',
  sourceExpense: 'Despesa de origem',
  originallyOwed: 'Valor originalmente devido',
  reimbursed: 'Reembolsado',
  remaining: 'Restante',
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

const countMessages = {
  'en-US': {
    openClaims: { one: 'Open claim', other: 'Open claims' },
    people: { one: 'Person', other: 'People' },
    openClaimsCount: { one: '{count} open claim', other: '{count} open claims' },
    installments: { one: '{count} installment', other: '{count} installments' },
  },
  'pt-BR': {
    openClaims: { one: 'Cobrança aberta', other: 'Cobranças abertas' },
    people: { one: 'Pessoa', other: 'Pessoas' },
    openClaimsCount: { one: '{count} cobrança aberta', other: '{count} cobranças abertas' },
    installments: { one: '{count} parcela', other: '{count} parcelas' },
  },
} as const

type ReimbursementCountMessageKey = keyof (typeof countMessages)['en-US']

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

export function reimbursementCountText(
  locale: AppLocale,
  key: ReimbursementCountMessageKey,
  count: number,
): string {
  return formatPluralMessage(locale, count, countMessages[locale][key])
}

export function reimbursementStatusLabel(status: ReimbursementStatus, locale: AppLocale): string {
  const labels: Record<ReimbursementStatus, Record<AppLocale, string>> = {
    PENDING: { 'en-US': 'Pending', 'pt-BR': 'Pendente' },
    PARTIALLY_PAID: { 'en-US': 'Partially paid', 'pt-BR': 'Parcialmente pago' },
    PAID: { 'en-US': 'Paid', 'pt-BR': 'Pago' },
  }
  return labels[status][locale]
}
