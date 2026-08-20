import type { AppLocale } from '@/app/providers/locale-provider'

const enUS = {
  tagline: 'Speak it. Track it. Understand it.',
  pageNotFound: 'Page not found',
  backToTalkTally: 'Back to TalkTally',
} as const

type CommonMessageKey = keyof typeof enUS

const ptBR: Record<CommonMessageKey, string> = {
  tagline: 'Fale. Registre. Entenda.',
  pageNotFound: 'Página não encontrada',
  backToTalkTally: 'Voltar ao TalkTally',
}

const messages: Record<AppLocale, Record<CommonMessageKey, string>> = {
  'en-US': enUS,
  'pt-BR': ptBR,
}

export function commonText(locale: AppLocale, key: CommonMessageKey): string {
  return messages[locale][key]
}
