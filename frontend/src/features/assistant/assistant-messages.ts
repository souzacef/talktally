import type { AppLocale } from '@/app/providers/locale-provider'
import type { AssistantStatus } from '@/types/api'

const enUS = {
  eyebrow: 'Voice intelligence',
  title: 'Assistant',
  subtitle: 'Capture and understand your money through the secured TalkTally assistant.',
  conversation: 'Conversation',
  conversationDescription: 'Messages are sent to the real Spring assistant API.',
  ready: 'Ready when you are',
  readyDescription: 'Type a message or use the microphone. No sample conversation is loaded.',
  thinking: 'Thinking…',
  message: 'Message',
  placeholder: 'Tell TalkTally what happened…',
  mobilePlaceholder: 'What happened?',
  sendMessage: 'Send message',
  speakTitle: 'Speak to TalkTally',
  speakDescription: 'Mono PCM16 WAV, sent securely',
  listening: 'Listening…',
  understanding: 'Understanding…',
  tapToSpeak: 'Tap to speak',
  recordingLimit: "The recording stops automatically before the backend's 8 MiB limit.",
  requestFailed: 'Assistant request failed',
  completed: 'Completed',
  needsClarification: 'Needs clarification',
  voiceUnavailable: 'Voice reply unavailable — result still succeeded.',
  voiceReply: 'Voice reply',
  audioUnsupported: 'Audio playback is not supported.',
} as const

type AssistantMessageKey = keyof typeof enUS

const ptBR: Record<AssistantMessageKey, string> = {
  eyebrow: 'Inteligência por voz',
  title: 'Assistente',
  subtitle: 'Registre e entenda seu dinheiro com o assistente protegido do TalkTally.',
  conversation: 'Conversa',
  conversationDescription: 'As mensagens são enviadas para a API real do assistente Spring.',
  ready: 'Pronto quando você estiver',
  readyDescription: 'Digite uma mensagem ou use o microfone. Nenhuma conversa de exemplo é carregada.',
  thinking: 'Pensando…',
  message: 'Mensagem',
  placeholder: 'Conte ao TalkTally o que aconteceu…',
  mobilePlaceholder: 'O que aconteceu?',
  sendMessage: 'Enviar mensagem',
  speakTitle: 'Fale com o TalkTally',
  speakDescription: 'WAV mono PCM16, enviado com segurança',
  listening: 'Ouvindo…',
  understanding: 'Entendendo…',
  tapToSpeak: 'Toque para falar',
  recordingLimit: 'A gravação para automaticamente antes do limite de 8 MiB do backend.',
  requestFailed: 'Falha na solicitação ao assistente',
  completed: 'Concluído',
  needsClarification: 'Precisa de esclarecimento',
  voiceUnavailable: 'Resposta por voz indisponível — o resultado foi concluído mesmo assim.',
  voiceReply: 'Resposta por voz',
  audioUnsupported: 'A reprodução de áudio não é compatível.',
}

const messages: Record<AppLocale, Record<AssistantMessageKey, string>> = {
  'en-US': enUS,
  'pt-BR': ptBR,
}

export function assistantText(locale: AppLocale, key: AssistantMessageKey): string {
  return messages[locale][key]
}

export function assistantStatusLabel(status: AssistantStatus, locale: AppLocale): string {
  return status === 'COMPLETED'
    ? assistantText(locale, 'completed')
    : assistantText(locale, 'needsClarification')
}
