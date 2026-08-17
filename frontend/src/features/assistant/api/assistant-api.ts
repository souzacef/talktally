import { apiClient } from '@/lib/api/api-client'
import type {
  AssistantMessageResponse,
  VoiceAssistantResponse,
} from '@/types/api'

export const assistantApi = {
  sendMessage: (message: string, signal?: AbortSignal) =>
    apiClient.post<AssistantMessageResponse>(
      '/api/v1/assistant/messages',
      { message },
      signal,
    ),
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
