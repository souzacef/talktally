import type { AppLocale } from '@/app/providers/locale-provider'
import type { AssistantStatus } from '@/types/api'

const enUS = {
  eyebrow: 'Voice intelligence',
  title: 'Assistant',
  subtitle: 'Capture and understand your money through the secured TalkTally assistant.',
  conversation: 'Conversation',
  conversationDescription: 'Ask about your finances or record something new.',
  loadingHistory: 'Loading conversation',
  historyUnavailable: 'Conversation unavailable',
  historyUnavailableDescription: 'Your saved conversation could not be loaded. Try again in a moment.',
  clearConversation: 'Clear conversation',
  clearConversationPrompt: 'Clear this conversation and start fresh?',
  clearConversationDescription: 'This removes the saved Assistant context. Your financial records are not affected.',
  clearingConversation: 'Clearing…',
  clearConversationFailed: 'The conversation could not be cleared.',
  confirmClearConversation: 'Clear conversation',
  cancel: 'Cancel',
  ready: 'Ready when you are',
  readyDescription: 'Type a message or use the microphone to get started.',
  thinking: 'Thinking…',
  message: 'Message',
  placeholder: 'Tell TalkTally what happened…',
  mobilePlaceholder: 'What happened?',
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
} as const

type AssistantMessageKey = keyof typeof enUS

const ptBR: Record<AssistantMessageKey, string> = {
  eyebrow: 'Inteligência por voz',
  title: 'Assistente',
  subtitle: 'Registre e entenda seu dinheiro com o assistente protegido do TalkTally.',
  conversation: 'Conversa',
  conversationDescription: 'Pergunte sobre suas finanças ou registre algo novo.',
  loadingHistory: 'Carregando conversa',
  historyUnavailable: 'Conversa indisponível',
  historyUnavailableDescription: 'Não foi possível carregar sua conversa salva. Tente novamente em instantes.',
  clearConversation: 'Limpar conversa',
  clearConversationPrompt: 'Limpar esta conversa e começar de novo?',
  clearConversationDescription: 'Isso remove o contexto salvo do Assistente. Seus registros financeiros não serão afetados.',
  clearingConversation: 'Limpando…',
  clearConversationFailed: 'Não foi possível limpar a conversa.',
  confirmClearConversation: 'Limpar conversa',
  cancel: 'Cancelar',
  ready: 'Pronto quando você estiver',
  readyDescription: 'Digite uma mensagem ou use o microfone para começar.',
  thinking: 'Pensando…',
  message: 'Mensagem',
  placeholder: 'Conte ao TalkTally o que aconteceu…',
  mobilePlaceholder: 'O que aconteceu?',
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
