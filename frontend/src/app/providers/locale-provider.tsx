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
