import type { AppLocale } from '@/app/providers/locale-provider'
import type { AssistantStatus } from '@/types/api'

const enUS = {
  eyebrow: 'Voice intelligence',
  title: 'Assistant',
  subtitle: 'Capture and understand your money through the secured TalkTally assistant.',
  conversation: 'Conversation',
  conversationDescription: 'Ask about your finances or record something new.',
  ready: 'Ready when you are',
  readyDescription: 'Type a message or use the microphone to get started.',
  thinking: 'Thinking…',
  message: 'Message',
  placeholder: 'Ask about your finances or record a transaction…',
  mobilePlaceholder: 'Ask or record a transaction…',
  sendMessage: 'Send message',
  speakTitle: 'Speak to TalkTally',
  speakDescription: 'Speak naturally and TalkTally will take it from there.',
  listening: 'Listening…',
  understanding: 'Understanding…',
  tapToSpeak: 'Tap to speak',
  recordingLimit: 'Recording stops automatically when needed.',
  requestFailed: 'Assistant request failed',
  completed: 'Completed',
  needsClarification: 'Needs clarification',
  voiceUnavailable: 'Voice reply unavailable. The result still succeeded.',
  voiceReply: 'Voice reply',
  audioUnsupported: 'Audio playback is not supported.',
  voiceStart: 'Start microphone recording',
  voiceStop: 'Stop microphone recording',
  voiceProcessing: 'Processing voice command',
  voiceCompleted: 'Voice command completed',
  voiceNeedsClarification: 'Voice command needs clarification',
  voiceReplyUnavailableLabel: 'Voice reply unavailable',
  voiceFailed: 'Voice command failed',
  voiceRecordAnother: 'Record another message',
  voiceTryAgain: 'Try recording again',
  noSpeechDetected: 'No speech was detected. Try again.',
  recordingTooShort: 'Recording was too short. Try again.',
} as const

type AssistantMessageKey = keyof typeof enUS

const ptBR: Record<AssistantMessageKey, string> = {
  eyebrow: 'Inteligência por voz',
  title: 'Assistente',
  subtitle: 'Registre e entenda seu dinheiro com o assistente protegido do TalkTally.',
  conversation: 'Conversa',
  conversationDescription: 'Pergunte sobre suas finanças ou registre algo novo.',
  ready: 'Pronto quando você estiver',
  readyDescription: 'Digite uma mensagem ou use o microfone para começar.',
  thinking: 'Pensando…',
  message: 'Mensagem',
  placeholder: 'Pergunte sobre suas finanças ou registre uma transação…',
  mobilePlaceholder: 'Pergunte ou registre uma transação…',
  sendMessage: 'Enviar mensagem',
  speakTitle: 'Fale com o TalkTally',
  speakDescription: 'Fale naturalmente e deixe o TalkTally cuidar do resto.',
  listening: 'Ouvindo…',
  understanding: 'Entendendo…',
  tapToSpeak: 'Toque para falar',
  recordingLimit: 'A gravação para automaticamente quando necessário.',
  requestFailed: 'Falha na solicitação ao assistente',
  completed: 'Concluído',
  needsClarification: 'Precisa de esclarecimento',
  voiceUnavailable: 'Resposta por voz indisponível. O resultado foi concluído mesmo assim.',
  voiceReply: 'Resposta por voz',
  audioUnsupported: 'A reprodução de áudio não é compatível.',
  voiceStart: 'Iniciar gravação do microfone',
  voiceStop: 'Parar gravação do microfone',
  voiceProcessing: 'Processando comando de voz',
  voiceCompleted: 'Comando de voz concluído',
  voiceNeedsClarification: 'Comando de voz precisa de esclarecimento',
  voiceReplyUnavailableLabel: 'Resposta por voz indisponível',
  voiceFailed: 'Falha no comando de voz',
  voiceRecordAnother: 'Gravar outra mensagem',
  voiceTryAgain: 'Tentar gravar novamente',
  noSpeechDetected: 'Nenhuma fala foi detectada. Tente novamente.',
  recordingTooShort: 'A gravação foi curta demais. Tente novamente.',
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
