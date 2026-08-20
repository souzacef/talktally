import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

export const LOCALE_KEY = 'talktally.locale'
export type AppLocale = 'en-US' | 'pt-BR'

const enUS = {
  'nav.home': 'Home',
  'nav.transactions': 'Transactions',
  'nav.owed': 'Owed to Me',
  'nav.assistant': 'Assistant',
  'shell.appearance': 'Appearance',
  'shell.language': 'Language',
  'shell.activeAccount': 'Active account',
  'shell.identityUnavailable': 'Identity unavailable',
  'shell.signOut': 'Sign out',
  'shell.financialWorkspace': 'Financial workspace',
  'shell.secureSession': 'Secure session',
  'shell.openAccountSettings': 'Open account settings',
  'shell.closeAccountSettings': 'Close account settings',
  'shell.account': 'Account',
  'theme.preference': 'Theme preference',
  'theme.system': 'System',
  'theme.light': 'Light',
  'theme.dark': 'Dark',
  'theme.use': 'Use {theme} theme',
  'theme.title': '{theme} theme',
  'locale.preference': 'Language preference',
  'locale.english': 'English',
  'locale.portuguese': 'Português',
  'dashboard.thisMonth': 'This month',
  'dashboard.greetingNamed': 'Hello, {name}!',
  'dashboard.greetingGeneric': 'Hello!',
  'dashboard.story': "Here's how your money is looking.",
  'dashboard.voiceFirstCapture': 'Voice capture',
  'dashboard.whatHappened': 'What would you like to track?',
  'dashboard.voiceDescription': 'Speak naturally to record a transaction or ask about your finances.',
  'dashboard.heard': 'Heard: {transcript}',
  'dashboard.voiceReplyUnavailable': 'Voice reply unavailable. The result still succeeded.',
  'dashboard.typeInstead': 'Type instead',
  'dashboard.summaryUnavailable': 'Summary unavailable',
  'dashboard.summaryUnavailableDescription': 'Your financial totals could not be loaded right now.',
  'dashboard.earnedIncome': 'Earned income',
  'dashboard.earnedIncomeNote': 'Income recorded for this period',
  'dashboard.expenses': 'Expenses',
  'dashboard.expensesNote': 'Spending recorded for this period',
  'dashboard.reimbursementsReceived': 'Reimbursements received',
  'dashboard.reimbursementsNote': 'Money returned to you, not income',
  'dashboard.netCashFlow': 'Net cash flow',
  'dashboard.netCashFlowNote': 'Your net movement for this period',
  'dashboard.monthlyCashFlow': 'Monthly cash flow',
  'dashboard.monthlyCashFlowDescription': 'Income and expenses from the last six months.',
  'dashboard.loadingMonthlyCashFlow': 'Loading monthly cash flow',
  'dashboard.cashFlowUnavailable': 'Cash flow unavailable',
  'dashboard.cashFlowUnavailableDescription': 'The monthly series could not be loaded.',
  'dashboard.noCashFlow': 'No cash-flow activity',
  'dashboard.noCashFlowDescription': 'Your monthly trend will appear after transactions are recorded.',
  'dashboard.loadingCashFlowChart': 'Loading cash-flow chart',
  'dashboard.owedToMe': 'Owed to me',
  'dashboard.totalOutstanding': 'Total outstanding',
  'dashboard.openClaims': 'Open claims',
  'dashboard.people': 'People',
  'dashboard.viewReimbursements': 'View reimbursements',
  'dashboard.spendingByCategory': 'Spending by category',
  'dashboard.categoriesUnavailable': 'Categories unavailable',
  'dashboard.categoriesUnavailableDescription': 'Category reporting could not be loaded.',
  'dashboard.noExpenses': 'No expenses yet',
  'dashboard.noExpensesDescription': 'Category totals will appear when expenses are recorded.',
  'dashboard.occurrences': '{count} occurrences',
  'dashboard.recentActivity': 'Recent activity',
  'dashboard.recentActivityDescription': 'Your latest financial activity',
  'dashboard.viewAll': 'View all',
  'dashboard.activityUnavailable': 'Activity unavailable',
  'dashboard.activityUnavailableDescription': 'Recent transactions could not be loaded.',
  'dashboard.noTransactions': 'No transactions yet',
  'dashboard.noTransactionsDescription': 'Use voice or the assistant to record your first transaction.',
  'dashboard.loadingCategory': 'Loading category…',
  'dashboard.viewTransaction': 'View transaction {description}',
  'dashboard.chartLabel': 'Monthly cash flow chart',
  'dashboard.income': 'Income',
} as const

type MessageKey = keyof typeof enUS

const ptBR: Record<MessageKey, string> = {
  'nav.home': 'Início',
  'nav.transactions': 'Transações',
  'nav.owed': 'A receber',
  'nav.assistant': 'Assistente',
  'shell.appearance': 'Aparência',
  'shell.language': 'Idioma',
  'shell.activeAccount': 'Conta ativa',
  'shell.identityUnavailable': 'Identidade indisponível',
  'shell.signOut': 'Sair',
  'shell.financialWorkspace': 'Espaço financeiro',
  'shell.secureSession': 'Sessão segura',
  'shell.openAccountSettings': 'Abrir configurações da conta',
  'shell.closeAccountSettings': 'Fechar configurações da conta',
  'shell.account': 'Conta',
  'theme.preference': 'Preferência de tema',
  'theme.system': 'Sistema',
  'theme.light': 'Claro',
  'theme.dark': 'Escuro',
  'theme.use': 'Usar tema {theme}',
  'theme.title': 'Tema {theme}',
  'locale.preference': 'Preferência de idioma',
  'locale.english': 'English',
  'locale.portuguese': 'Português',
  'dashboard.thisMonth': 'Este mês',
  'dashboard.greetingNamed': 'Olá, {name}!',
  'dashboard.greetingGeneric': 'Olá!',
  'dashboard.story': 'Veja como estão suas finanças.',
  'dashboard.voiceFirstCapture': 'Registro por voz',
  'dashboard.whatHappened': 'O que você gostaria de registrar?',
  'dashboard.voiceDescription': 'Fale naturalmente para registrar uma transação ou perguntar sobre suas finanças.',
  'dashboard.heard': 'Ouvido: {transcript}',
  'dashboard.voiceReplyUnavailable': 'Resposta por voz indisponível. O resultado foi concluído mesmo assim.',
  'dashboard.typeInstead': 'Prefiro digitar',
  'dashboard.summaryUnavailable': 'Resumo indisponível',
  'dashboard.summaryUnavailableDescription': 'Não foi possível carregar seus totais financeiros agora.',
  'dashboard.earnedIncome': 'Renda recebida',
  'dashboard.earnedIncomeNote': 'Renda registrada neste período',
  'dashboard.expenses': 'Despesas',
  'dashboard.expensesNote': 'Gastos registrados neste período',
  'dashboard.reimbursementsReceived': 'Reembolsos recebidos',
  'dashboard.reimbursementsNote': 'Dinheiro devolvido a você, não é renda',
  'dashboard.netCashFlow': 'Fluxo de caixa líquido',
  'dashboard.netCashFlowNote': 'Sua movimentação líquida neste período',
  'dashboard.monthlyCashFlow': 'Fluxo de caixa mensal',
  'dashboard.monthlyCashFlowDescription': 'Renda e despesas dos últimos seis meses.',
  'dashboard.loadingMonthlyCashFlow': 'Carregando fluxo de caixa mensal',
  'dashboard.cashFlowUnavailable': 'Fluxo de caixa indisponível',
  'dashboard.cashFlowUnavailableDescription': 'Não foi possível carregar a série mensal.',
  'dashboard.noCashFlow': 'Sem movimentação no fluxo de caixa',
  'dashboard.noCashFlowDescription': 'Sua tendência mensal aparecerá depois que houver transações registradas.',
  'dashboard.loadingCashFlowChart': 'Carregando gráfico de fluxo de caixa',
  'dashboard.owedToMe': 'A receber',
  'dashboard.totalOutstanding': 'Total pendente',
  'dashboard.openClaims': 'Cobranças abertas',
  'dashboard.people': 'Pessoas',
  'dashboard.viewReimbursements': 'Ver reembolsos',
  'dashboard.spendingByCategory': 'Gastos por categoria',
  'dashboard.categoriesUnavailable': 'Categorias indisponíveis',
  'dashboard.categoriesUnavailableDescription': 'Não foi possível carregar o relatório por categoria.',
  'dashboard.noExpenses': 'Nenhuma despesa ainda',
  'dashboard.noExpensesDescription': 'Os totais por categoria aparecerão quando houver despesas registradas.',
  'dashboard.occurrences': '{count} ocorrências',
  'dashboard.recentActivity': 'Atividade recente',
  'dashboard.recentActivityDescription': 'Sua atividade financeira mais recente',
  'dashboard.viewAll': 'Ver todas',
  'dashboard.activityUnavailable': 'Atividade indisponível',
  'dashboard.activityUnavailableDescription': 'Não foi possível carregar as transações recentes.',
  'dashboard.noTransactions': 'Nenhuma transação ainda',
  'dashboard.noTransactionsDescription': 'Use a voz ou o assistente para registrar sua primeira transação.',
  'dashboard.loadingCategory': 'Carregando categoria…',
  'dashboard.viewTransaction': 'Ver transação {description}',
  'dashboard.chartLabel': 'Gráfico de fluxo de caixa mensal',
  'dashboard.income': 'Renda',
}

const messages: Record<AppLocale, Record<MessageKey, string>> = {
  'en-US': enUS,
  'pt-BR': ptBR,
}

type Params = Record<string, string | number>

interface LocaleContextValue {
  locale: AppLocale
  setLocale: (locale: AppLocale) => void
  t: (key: MessageKey, params?: Params) => string
  formatMoney: (value: number | string) => string
  formatDate: (value: string | Date, options?: Intl.DateTimeFormatOptions) => string
  formatMonthYear: (value: string | Date) => string
}

const LocaleContext = createContext<LocaleContextValue | null>(null)

function isLocale(value: string | null): value is AppLocale {
  return value === 'en-US' || value === 'pt-BR'
}

function browserLocale(): AppLocale {
  return navigator.language.toLowerCase().startsWith('pt') ? 'pt-BR' : 'en-US'
}

function parseDate(value: string | Date): Date {
  if (value instanceof Date) return value
  return /^\d{4}-\d{2}-\d{2}$/.test(value)
    ? new Date(`${value}T00:00:00Z`)
    : new Date(value)
}

function interpolate(message: string, params?: Params): string {
  if (!params) return message
  return message.replace(/\{(\w+)\}/g, (match, key: string) => (
    Object.prototype.hasOwnProperty.call(params, key) ? String(params[key]) : match
  ))
}

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<AppLocale>(() => {
    const stored = window.localStorage.getItem(LOCALE_KEY)
    return isLocale(stored) ? stored : browserLocale()
  })

  useEffect(() => {
    document.documentElement.lang = locale
  }, [locale])

  const value = useMemo<LocaleContextValue>(() => {
    const money = new Intl.NumberFormat(locale, { style: 'currency', currency: 'BRL' })
    const monthYear = new Intl.DateTimeFormat(locale, {
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC',
    })

    return {
      locale,
      setLocale: (nextLocale) => {
        window.localStorage.setItem(LOCALE_KEY, nextLocale)
        setLocaleState(nextLocale)
      },
      t: (key, params) => interpolate(messages[locale][key], params),
      formatMoney: (amount) => money.format(Number(amount)),
      formatDate: (date, options = {}) => new Intl.DateTimeFormat(locale, {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        timeZone: 'UTC',
        ...options,
      }).format(parseDate(date)),
      formatMonthYear: (date) => monthYear.format(parseDate(date)),
    }
  }, [locale])

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>
}

export function useLocale(): LocaleContextValue {
  const context = useContext(LocaleContext)
  if (!context) throw new Error('useLocale must be used within LocaleProvider')
  return context
}
