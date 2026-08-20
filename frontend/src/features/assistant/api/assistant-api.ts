import { apiClient } from '@/lib/api/api-client'
import type {
  AssistantConversationMessageResponse,
  AssistantMessageResponse,
  VoiceAssistantResponse,
} from '@/types/api'

const MESSAGE_PATH = '/api/v1/assistant/messages'

export const assistantApi = {
  history: (signal?: AbortSignal) =>
    apiClient.get<AssistantConversationMessageResponse[]>(MESSAGE_PATH, { signal }),
  sendMessage: (message: string, signal?: AbortSignal) =>
    apiClient.post<AssistantMessageResponse>(
      MESSAGE_PATH,
      { message },
      signal,
    ),
  clearHistory: (signal?: AbortSignal) => apiClient.delete(MESSAGE_PATH, signal),
  sendVoice: (wav: Blob, signal?: AbortSignal) => {
    const formData = new FormData()
    formData.append('file', wav, 'talktally-command.wav')
    return apiClient.postForm<VoiceAssistantResponse>(
      '/api/v1/assistant/voice',
      formData,
      signal,
    )
  },
}
